import Foundation

/// Build-time configuration. Change `baseURL` to point at your backend.
enum AppConfig {
    /// Base URL of the smart-estate Spring backend.
    ///
    /// - Simulator can reach the Mac's own localhost directly (default below).
    /// - On a REAL device, set this to your Mac's LAN IP (e.g. `http://192.168.1.20:8080`)
    ///   or your Lightsail host; a device can't see the simulator's localhost.
    static let baseURL = URL(string: "http://localhost:8080")!

    /// APNs environment reported to the backend at device registration: SANDBOX or PRODUCTION.
    /// Debug builds use the sandbox APNs; keep this SANDBOX until you ship a release build.
    static let pushEnvironment = "SANDBOX"

    static var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }
}
