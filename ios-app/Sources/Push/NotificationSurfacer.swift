import Foundation
import UserNotifications

/// Raises local banners + sound for new, unread, non-emergency inbox notices when a real APNs push
/// wasn't delivered (mock backend, simulator, or the notice went out on SMS). Emergencies use the
/// full-screen alarm and are skipped here.
///
/// Dedup state is in-memory, per app session: the first pass primes it silently so the backlog
/// doesn't all buzz, then banners fire for later arrivals. Not persisted, because the backend's
/// in-memory DB reuses notification ids across restarts and a persisted set would suppress fresh notices.
@MainActor
final class NotificationSurfacer {
    static let shared = NotificationSurfacer()
    private init() {}

    private var seen = Set<Int>()
    private var primed = false

    /// Record that a notice has already been surfaced (e.g. a real push arrived for it), so the
    /// poll won't raise a duplicate local banner for the same id.
    func markSeen(_ id: Int) { seen.insert(id) }

    /// Given the latest inbox, raise a local banner for any new, unread, non-emergency notice.
    func surface(_ items: [NotificationItem]) {
        for item in items where !seen.contains(item.id) {
            seen.insert(item.id)
            if primed && !item.read && !item.isCritical {
                postLocal(item)
            }
        }
        primed = true
    }

    private func postLocal(_ item: NotificationItem) {
        let content = UNMutableNotificationContent()
        content.title = item.title.isEmpty ? "Smart Estate" : item.title
        content.body = item.body
        content.sound = .default
        content.userInfo = ["notificationId": item.id]
        let request = UNNotificationRequest(identifier: "notice-\(item.id)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
