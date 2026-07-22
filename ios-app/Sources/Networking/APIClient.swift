import Foundation

enum APIError: Error {
    case unauthorized
    case server(Int)
    case invalidResponse
    case transport(Error)
}

/// Thin async client for the smart-estate mobile REST API (`/api/mobile/**`). Attaches the bearer
/// token to authorized requests and maps 401s to `APIError.unauthorized` so the UI can sign out.
final class APIClient {
    static let shared = APIClient()
    private init() {}

    var baseURL = AppConfig.baseURL
    var token: String?

    // MARK: Endpoints

    func login(email: String, password: String) async throws -> LoginResponse {
        let req = try makeRequest("api/mobile/auth/login", method: "POST",
                                  body: LoginRequest(email: email, password: password), authorized: false)
        let data = try await send(req)
        return try JSONDecoder().decode(LoginResponse.self, from: data)
    }

    func logout() async {
        guard let req = try? makeRequest("api/mobile/auth/logout", method: "POST") else { return }
        _ = try? await send(req)
    }

    func me() async throws -> User {
        let data = try await send(try makeRequest("api/mobile/me"))
        return try JSONDecoder().decode(User.self, from: data)
    }

    func registerDevice(deviceToken: String) async throws {
        let body = DeviceRegistration(token: deviceToken, platform: "IOS",
                                      environment: AppConfig.pushEnvironment, appVersion: AppConfig.appVersion)
        _ = try await send(try makeRequest("api/mobile/devices", method: "POST", body: body))
    }

    func unregisterDevice(_ deviceToken: String) async {
        guard let req = try? makeRequest("api/mobile/devices/\(deviceToken)", method: "DELETE") else { return }
        _ = try? await send(req)
    }

    func notifications() async throws -> [NotificationItem] {
        let data = try await send(try makeRequest("api/mobile/notifications"))
        return try JSONDecoder().decode([NotificationItem].self, from: data)
    }

    func markRead(id: Int) async {
        guard let req = try? makeRequest("api/mobile/notifications/\(id)/read", method: "POST") else { return }
        _ = try? await send(req)
    }

    func activeEmergency() async throws -> EmergencyItem? {
        let data = try await send(try makeRequest("api/mobile/emergency/active"))
        if data.isEmpty { return nil } // 204 No Content
        return try? JSONDecoder().decode(EmergencyItem.self, from: data)
    }

    // MARK: Plumbing

    private func makeRequest(_ path: String, method: String = "GET",
                             body: Encodable? = nil, authorized: Bool = true) throws -> URLRequest {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = try JSONEncoder().encode(AnyEncodable(body))
        }
        if authorized, let token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return req
    }

    private func send(_ req: URLRequest) async throws -> Data {
        do {
            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse else { throw APIError.invalidResponse }
            switch http.statusCode {
            case 200..<300: return data
            case 401:       throw APIError.unauthorized
            default:        throw APIError.server(http.statusCode)
            }
        } catch let e as APIError {
            throw e
        } catch {
            throw APIError.transport(error)
        }
    }
}

/// Type-erasing wrapper so an existential `Encodable` can be JSON-encoded.
private struct AnyEncodable: Encodable {
    private let encodeFunc: (Encoder) throws -> Void
    init(_ wrapped: Encodable) { self.encodeFunc = wrapped.encode }
    func encode(to encoder: Encoder) throws { try encodeFunc(encoder) }
}
