import Foundation

// MARK: Requests

struct LoginRequest: Encodable {
    let email: String
    let password: String
}

struct DeviceRegistration: Encodable {
    let token: String
    let platform: String
    let environment: String
    let appVersion: String
}

// MARK: Responses

struct User: Codable, Identifiable {
    let id: Int
    let name: String
    let email: String
    let role: String
    let language: String?
}

struct LoginResponse: Decodable {
    let token: String
    let user: User
}

/// One inbox item. Decoding is lenient: unexpected shapes (e.g. a date serialized as an array
/// rather than an ISO string) degrade to nil instead of failing the whole list.
struct NotificationItem: Decodable, Identifiable {
    let id: Int
    let title: String
    let body: String
    let category: String?
    let severity: String?
    let emergency: Bool
    let status: String?
    let read: Bool
    let sentAt: String?

    var isCritical: Bool {
        emergency || severity == "CRITICAL" || category == "EMERGENCY"
    }

    enum CodingKeys: String, CodingKey {
        case id, title, body, category, severity, emergency, status, read, sentAt
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        title = (try? c.decode(String.self, forKey: .title)) ?? ""
        body = (try? c.decode(String.self, forKey: .body)) ?? ""
        category = try? c.decodeIfPresent(String.self, forKey: .category)
        severity = try? c.decodeIfPresent(String.self, forKey: .severity)
        emergency = (try? c.decode(Bool.self, forKey: .emergency)) ?? false
        status = try? c.decodeIfPresent(String.self, forKey: .status)
        read = (try? c.decode(Bool.self, forKey: .read)) ?? false
        sentAt = try? c.decodeIfPresent(String.self, forKey: .sentAt)
    }
}

struct EmergencyItem: Decodable, Identifiable {
    let id: Int
    let type: String?
    let label: String?
    let declaredAt: String?
    let note: String?
}
