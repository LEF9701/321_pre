import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var auth: AuthStore
    @EnvironmentObject var push: PushManager
    @EnvironmentObject var loc: LocalizationManager

    var body: some View {
        NavigationStack {
            Form {
                Section(loc.t("settings.account")) {
                    LabeledContent(loc.t("settings.name"), value: auth.user?.name ?? "—")
                    LabeledContent(loc.t("settings.email"), value: auth.user?.email ?? "—")
                    LabeledContent(loc.t("settings.role"), value: auth.user?.role ?? "—")
                }

                Section(loc.t("settings.push")) {
                    LabeledContent(loc.t("settings.permission"),
                                   value: push.authorizationGranted ? loc.t("settings.granted") : loc.t("settings.notGranted"))
                    LabeledContent(loc.t("settings.device"),
                                   value: push.currentDeviceToken == nil ? loc.t("settings.pending") : loc.t("settings.registered"))
                }

                Section(loc.t("settings.language")) {
                    Picker(loc.t("settings.language"), selection: Binding(
                        get: { loc.language },
                        set: { loc.set($0) }
                    )) {
                        ForEach(AppLanguage.allCases) { lang in
                            Text(lang.displayName(loc)).tag(lang)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section(loc.t("settings.demo")) {
                    Button {
                        AlarmController.shared.start(
                            title: loc.t("alarm.fireTitle"),
                            body: loc.t("alarm.fireBody"),
                            notificationId: nil)
                    } label: {
                        Label(loc.t("settings.testAlarm"), systemImage: "flame.fill")
                    }
                    .tint(.red)
                }

                Section {
                    Button(loc.t("settings.signOut"), role: .destructive) {
                        Task { await auth.signOut() }
                    }
                }
            }
            .navigationTitle(loc.t("settings.title"))
        }
    }
}
