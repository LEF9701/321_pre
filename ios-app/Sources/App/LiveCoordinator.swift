import Foundation

extension Notification.Name {
    /// Posted to tell views (the inbox) to reload from the backend. Emitted by the RootView poll
    /// loop, on returning to the foreground, and whenever a push arrives (see NotificationRouter).
    static let smartEstateRefresh = Notification.Name("smartEstate.refresh")
}
