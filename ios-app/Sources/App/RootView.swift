import SwiftUI

/// Top-level view: routes between loading / login / main tabs, and presents the full-screen fire
/// alarm over everything when `AlarmController` is active.
struct RootView: View {
    @EnvironmentObject var auth: AuthStore
    @EnvironmentObject var alarm: AlarmController
    @EnvironmentObject var loc: LocalizationManager
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            switch auth.state {
            case .unknown:
                ProgressView(loc.t("common.loading"))
            case .signedOut:
                LoginView()
            case .signedIn:
                MainTabView()
            }
        }
        .fullScreenCover(isPresented: $alarm.isAlarming) {
            AlarmView()
        }
        // While signed in, poll the backend on a loop so the inbox stays fresh and a declared
        // emergency raises the alarm even if the APNs push was missed or delayed.
        .task(id: auth.state == .signedIn) {
            guard auth.state == .signedIn else { return }
            while !Task.isCancelled {
                await AlarmController.shared.checkActiveEmergency()
                NotificationCenter.default.post(name: .smartEstateRefresh, object: nil)
                try? await Task.sleep(nanoseconds: 15_000_000_000) // 15s
            }
        }
        // Refresh immediately when returning to the foreground.
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active, auth.state == .signedIn else { return }
            NotificationCenter.default.post(name: .smartEstateRefresh, object: nil)
            Task { await AlarmController.shared.checkActiveEmergency() }
        }
    }
}

struct MainTabView: View {
    @EnvironmentObject var loc: LocalizationManager
    var body: some View {
        TabView {
            InboxView()
                .tabItem { Label(loc.t("tab.alerts"), systemImage: "bell.fill") }
            SettingsView()
                .tabItem { Label(loc.t("tab.settings"), systemImage: "gearshape.fill") }
        }
    }
}
