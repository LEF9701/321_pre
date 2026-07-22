import SwiftUI

/// App entry point. Wires the UIKit AppDelegate (needed for APNs registration + notification
/// delegate callbacks) into the SwiftUI lifecycle, and injects the shared stores.
@main
struct SmartEstatePushApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var auth = AuthStore()
    @StateObject private var alarm = AlarmController.shared
    @StateObject private var push = PushManager.shared
    @StateObject private var loc = LocalizationManager.shared

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .environmentObject(alarm)
                .environmentObject(push)
                .environmentObject(loc)
                // Drive SwiftUI's own formatting off the chosen UI language, and re-render the
                // whole tree when it changes.
                .environment(\.locale, loc.locale)
                .id(loc.language)
        }
    }
}
