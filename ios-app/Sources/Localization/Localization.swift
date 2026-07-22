import SwiftUI

/// The app's display language. `.system` follows the device; the other two are an explicit
/// in-app override so a resident can read the app in English or Traditional Chinese regardless
/// of their phone's language (FR §5.1, bilingual support).
///
/// This is the app *chrome* language only. Notification title/body text is delivered by the
/// backend already localised to the resident's account language, so incoming push content is
/// rendered as received and never re-translated on the device.
enum AppLanguage: String, CaseIterable, Identifiable {
    case system
    case en
    case zhHant = "zh-Hant"

    var id: String { rawValue }

    /// Each option names itself in its own language; only "Follow system" is translated.
    @MainActor
    func displayName(_ loc: LocalizationManager) -> String {
        switch self {
        case .system: return loc.t("lang.system")
        case .en:     return "English"
        case .zhHant: return "繁體中文"
        }
    }
}

/// Loads UI strings from the `.lproj` bundle for the selected language and re-renders the whole
/// view tree when the choice changes. Injected as an `@EnvironmentObject`; non-view code
/// (AlarmController, NotificationRouter) reaches it through `LocalizationManager.shared`.
@MainActor
final class LocalizationManager: ObservableObject {
    static let shared = LocalizationManager()

    @Published private(set) var language: AppLanguage

    private let storageKey = "app_language"

    private init() {
        let saved = UserDefaults.standard.string(forKey: storageKey)
        language = AppLanguage(rawValue: saved ?? "") ?? .system
    }

    func set(_ lang: AppLanguage) {
        guard lang != language else { return }
        language = lang
        UserDefaults.standard.set(lang.rawValue, forKey: storageKey)
    }

    /// The locale to hand SwiftUI (`.environment(\.locale, ...)`) so dates/numbers match the UI.
    var locale: Locale {
        switch language {
        case .system: return .current
        case .en:     return Locale(identifier: "en")
        case .zhHant: return Locale(identifier: "zh-Hant")
        }
    }

    /// The resource bundle whose strings we read. For `.system` we resolve the device's best match.
    private var bundle: Bundle {
        let code: String
        switch language {
        case .system: code = Bundle.main.preferredLocalizations.first ?? "en"
        case .en:     code = "en"
        case .zhHant: code = "zh-Hant"
        }
        if let path = Bundle.main.path(forResource: code, ofType: "lproj"),
           let b = Bundle(path: path) {
            return b
        }
        return .main
    }

    /// Localise a key, filling `%@` positional arguments. Falls back to the key itself so a
    /// missing entry is visible rather than blank.
    func t(_ key: String, _ args: CVarArg...) -> String {
        let format = bundle.localizedString(forKey: key, value: key, table: nil)
        return args.isEmpty ? format : String(format: format, locale: locale, arguments: args)
    }

    /// Localised label for a server enum NAME (e.g. "EMERGENCY", "CRITICAL", "TYPHOON"),
    /// mirroring the web UI's enums.js. Falls back to a readable form of the raw value.
    func enumLabel(_ raw: String?) -> String {
        guard let raw, !raw.isEmpty else { return "" }
        let key = "enum.\(raw)"
        let hit = bundle.localizedString(forKey: key, value: key, table: nil)
        if hit != key { return hit }
        return raw.replacingOccurrences(of: "_", with: " ").capitalized
    }
}
