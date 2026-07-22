import Foundation
import UIKit
import UserNotifications

/// Owns push permission + the APNs device token, and registers that token with the backend once the
/// user is signed in. Also defines the FIRE_ALARM notification category (with an Acknowledge action).
@MainActor
final class PushManager: ObservableObject {
    static let shared = PushManager()
    private init() {}

    static let fireCategoryId = "FIRE_ALARM"
    static let ackActionId = "ACK"

    @Published var authorizationGranted = false
    /// APNs device token as a hex string, once iOS provides it.
    @Published private(set) var currentDeviceToken: String?

    func configureCategories() {
        let ack = UNNotificationAction(identifier: Self.ackActionId,
                                       title: "I'm safe / Acknowledge",
                                       options: [.foreground])
        let fire = UNNotificationCategory(identifier: Self.fireCategoryId,
                                          actions: [ack],
                                          intentIdentifiers: [],
                                          options: [.customDismissAction])
        UNUserNotificationCenter.current().setNotificationCategories([fire])
    }

    func requestAuthorizationAndRegister() async {
        let center = UNUserNotificationCenter.current()
        do {
            authorizationGranted = try await center.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            authorizationGranted = false
        }
        // Register for remote notifications regardless; the token is still useful.
        UIApplication.shared.registerForRemoteNotifications()
    }

    func didRegister(deviceToken: Data) {
        currentDeviceToken = deviceToken.map { String(format: "%02x", $0) }.joined()
        Task { await registerPendingTokenIfNeeded() }
    }

    /// Push the current token to the backend if we have both a token and a signed-in session.
    func registerPendingTokenIfNeeded() async {
        guard let token = currentDeviceToken, APIClient.shared.token != nil else { return }
        try? await APIClient.shared.registerDevice(deviceToken: token)
    }
}
