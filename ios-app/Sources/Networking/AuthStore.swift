import Foundation

/// Owns authentication state for the app: the bearer token (persisted in the Keychain) and the
/// signed-in user. Drives whether the UI shows login or the main tabs.
@MainActor
final class AuthStore: ObservableObject {
    enum State {
        case unknown    // still checking stored token
        case signedOut
        case signedIn
    }

    @Published var state: State = .unknown
    @Published var user: User?
    @Published var errorMessage: String?
    @Published var isBusy = false

    private let tokenAccount = "authToken"

    init() {
        #if DEBUG
        // Test-only: sign in from a token passed via SIMCTL_CHILD_SE_DEBUG_TOKEN so the app can be
        // driven headlessly in the simulator. Compiled out of release builds.
        if let debugToken = ProcessInfo.processInfo.environment["SE_DEBUG_TOKEN"], !debugToken.isEmpty {
            APIClient.shared.token = debugToken
            state = .signedIn
            Task { await refreshMe() }
            return
        }
        #endif
        if let saved = KeychainHelper.read(account: tokenAccount) {
            APIClient.shared.token = saved
            state = .signedIn
            Task { await refreshMe() }
        } else {
            state = .signedOut
        }
    }

    func login(email: String, password: String) async {
        isBusy = true
        errorMessage = nil
        defer { isBusy = false }
        do {
            let resp = try await APIClient.shared.login(email: email, password: password)
            APIClient.shared.token = resp.token
            KeychainHelper.save(resp.token, account: tokenAccount)
            user = resp.user
            state = .signedIn
            await PushManager.shared.registerPendingTokenIfNeeded()
        } catch APIError.unauthorized {
            errorMessage = LocalizationManager.shared.t("login.invalidCredentials")
        } catch {
            errorMessage = LocalizationManager.shared.t("login.signInFailed")
        }
    }

    func refreshMe() async {
        do {
            user = try await APIClient.shared.me()
        } catch APIError.unauthorized {
            await signOut(remote: false)
        } catch {
            // keep any cached user; transient network error
        }
    }

    func signOut(remote: Bool = true) async {
        if let deviceToken = PushManager.shared.currentDeviceToken {
            await APIClient.shared.unregisterDevice(deviceToken)
        }
        if remote {
            await APIClient.shared.logout()
        }
        APIClient.shared.token = nil
        KeychainHelper.delete(account: tokenAccount)
        user = nil
        state = .signedOut
    }
}
