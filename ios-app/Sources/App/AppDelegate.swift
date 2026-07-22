import UIKit
import UserNotifications

/// Handles APNs registration and incoming notifications. Foreground arrivals and taps are routed
/// through `NotificationRouter`, which starts the full-screen fire alarm for critical pushes.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        Task { @MainActor in
            PushManager.shared.configureCategories()
            await PushManager.shared.requestAuthorizationAndRegister()
        }
        return true
    }

    // MARK: APNs token

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Task { @MainActor in PushManager.shared.didRegister(deviceToken: deviceToken) }
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("APNs registration failed: \(error)")
    }

    // MARK: Incoming notifications

    /// Notification arrived while the app is in the foreground.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification) async
    -> UNNotificationPresentationOptions {
        let userInfo = notification.request.content.userInfo
        await MainActor.run { NotificationRouter.handle(userInfo: userInfo, fromTap: false) }
        return [.banner, .sound, .list]
    }

    /// User tapped the notification or one of its actions.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse) async {
        if response.actionIdentifier == PushManager.ackActionId {
            await MainActor.run { AlarmController.shared.acknowledge() }
            return
        }
        let userInfo = response.notification.request.content.userInfo
        await MainActor.run { NotificationRouter.handle(userInfo: userInfo, fromTap: true) }
    }
}
