import Foundation

/// Inspects an APNs payload and decides what to do: fire pushes (the backend sets `alarm: true`,
/// plus category/severity/emergencyType) launch the full-screen alarm; normal pushes are just
/// marked read on tap. The keys mirror what `PushSender` puts in the payload on the backend.
enum NotificationRouter {

    @MainActor
    static func handle(userInfo: [AnyHashable: Any], fromTap: Bool) {
        let isAlarm = boolValue(userInfo["alarm"])
            || (userInfo["category"] as? String == "EMERGENCY")
            || (userInfo["severity"] as? String == "CRITICAL")
            || (userInfo["emergencyType"] != nil)

        let notificationId = intValue(userInfo["notificationId"])
        let emergencyId = intValue(userInfo["emergencyEventId"])
        let (title, body) = alertText(userInfo)

        if isAlarm {
            AlarmController.shared.start(title: title, body: body,
                                        notificationId: notificationId, emergencyId: emergencyId)
        } else if fromTap, let id = notificationId {
            Task { await APIClient.shared.markRead(id: id) }
        }

        // A real push already alerted the user for this notice; don't let the poll double-surface it.
        if let id = notificationId { NotificationSurfacer.shared.markSeen(id) }

        // Any incoming push should freshen the inbox so it isn't stale.
        NotificationCenter.default.post(name: .smartEstateRefresh, object: nil)
    }

    private static func alertText(_ userInfo: [AnyHashable: Any]) -> (String, String) {
        guard let aps = userInfo["aps"] as? [String: Any] else { return ("", "") }
        if let alert = aps["alert"] as? [String: Any] {
            return (alert["title"] as? String ?? "", alert["body"] as? String ?? "")
        }
        if let alert = aps["alert"] as? String {
            return ("", alert)
        }
        return ("", "")
    }

    private static func boolValue(_ any: Any?) -> Bool {
        if let b = any as? Bool { return b }
        if let n = any as? NSNumber { return n.boolValue }
        if let s = any as? String { return s.lowercased() == "true" || s == "1" }
        return false
    }

    private static func intValue(_ any: Any?) -> Int? {
        if let i = any as? Int { return i }
        if let n = any as? NSNumber { return n.intValue }
        if let s = any as? String { return Int(s) }
        return nil
    }
}
