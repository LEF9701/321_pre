import SwiftUI

struct LoginView: View {
    @EnvironmentObject var auth: AuthStore
    @EnvironmentObject var loc: LocalizationManager
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        NavigationStack {
            Form {
                Section(loc.t("login.section")) {
                    TextField(loc.t("login.email"), text: $email)
                        .textContentType(.username)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    SecureField(loc.t("login.password"), text: $password)
                        .textContentType(.password)
                }

                if let msg = auth.errorMessage {
                    Text(msg)
                        .foregroundStyle(.red)
                        .font(.footnote)
                }

                Section {
                    Button {
                        Task { await auth.login(email: email, password: password) }
                    } label: {
                        HStack {
                            Spacer()
                            if auth.isBusy { ProgressView() } else { Text(loc.t("login.signIn")).bold() }
                            Spacer()
                        }
                    }
                    .disabled(auth.isBusy || email.isEmpty || password.isEmpty)
                }

                Section {
                    Text(loc.t("login.server", AppConfig.baseURL.absoluteString))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle(loc.t("login.title"))
        }
    }
}
