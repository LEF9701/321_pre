import Foundation
import AVFoundation
import CoreHaptics
import UIKit

/// Drives the fire-alarm experience: a looping siren that plays even when the ring/silent switch is
/// on (via an `.playback` audio session, active while the app is foreground), continuous haptics,
/// and the state that shows the full-screen `AlarmView`. Stops on acknowledgement.
@MainActor
final class AlarmController: ObservableObject {
    static let shared = AlarmController()
    private init() {}

    @Published var isAlarming = false
    @Published var title = ""
    @Published var body = ""
    private(set) var notificationId: Int?
    private var currentEmergencyId: Int?
    private var lastAcknowledgedEmergencyId: Int?

    private var player: AVAudioPlayer?
    private var hapticEngine: CHHapticEngine?
    private var hapticTimer: Timer?

    func start(title: String, body: String, notificationId: Int? = nil, emergencyId: Int? = nil) {
        self.title = title.isEmpty ? LocalizationManager.shared.t("alarm.defaultTitle") : title
        self.body = body
        self.notificationId = notificationId
        self.currentEmergencyId = emergencyId
        isAlarming = true
        startSiren()
        startHaptics()
    }

    func acknowledge() {
        if let eid = currentEmergencyId { lastAcknowledgedEmergencyId = eid }
        stopSiren()
        stopHaptics()
        isAlarming = false
        if let id = notificationId {
            Task { await APIClient.shared.markRead(id: id) }
        }
        notificationId = nil
        currentEmergencyId = nil
    }

    /// Poll the backend for an active emergency and raise the alarm if one is active and hasn't
    /// already been acknowledged on this device. Fallback so a declared fire alarm still takes over
    /// the app if the APNs push was missed or delayed.
    func checkActiveEmergency() async {
        guard !isAlarming else { return }
        let active: EmergencyItem?
        do { active = try await APIClient.shared.activeEmergency() } catch { return }
        guard let e = active, e.id != lastAcknowledgedEmergencyId else { return }
        let (t, b) = Self.alarmText(for: e)
        start(title: t, body: b, notificationId: nil, emergencyId: e.id)
    }

    private static func alarmText(for e: EmergencyItem) -> (String, String) {
        // App-generated fallback wording is localised; a note typed by staff is shown verbatim.
        let loc = LocalizationManager.shared
        let hasNote = (e.note?.isEmpty == false)
        if (e.type ?? "").uppercased() == "FIRE" {
            return (loc.t("alarm.fireTitle"),
                    hasNote ? e.note! : loc.t("alarm.fireBody"))
        }
        // Prefer the emergency TYPE enum name so the label is localised; fall back to the
        // server-sent English label, then a generic default.
        let typeLabel = (e.type?.isEmpty == false) ? loc.enumLabel(e.type) : (e.label ?? loc.t("alarm.defaultTitle"))
        return (loc.t("alarm.genericTitle", typeLabel),
                hasNote ? e.note! : loc.t("alarm.genericBody"))
    }

    // MARK: Siren

    private func startSiren() {
        guard let url = Bundle.main.url(forResource: "fire_alarm", withExtension: "caf") else { return }
        do {
            // .playback + active plays through the ring/silent switch while the app is in front.
            try AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            let p = try AVAudioPlayer(contentsOf: url)
            p.numberOfLoops = -1
            p.volume = 1.0
            p.prepareToPlay()
            p.play()
            player = p
        } catch {
            // Audio unavailable; haptics + the flashing UI still convey the alarm.
        }
    }

    private func stopSiren() {
        player?.stop()
        player = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }

    // MARK: Haptics

    private func startHaptics() {
        if CHHapticEngine.capabilitiesForHardware().supportsHaptics {
            do {
                let engine = try CHHapticEngine()
                try engine.start()
                hapticEngine = engine
                playHapticBurst()
            } catch {
                startFallbackHaptics()
            }
        } else {
            startFallbackHaptics()
        }
    }

    private func playHapticBurst() {
        guard let engine = hapticEngine else { return }
        do {
            var events: [CHHapticEvent] = []
            var t = 0.0
            for _ in 0..<8 {
                events.append(CHHapticEvent(eventType: .hapticTransient, parameters: [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: 1.0),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: 1.0)
                ], relativeTime: t))
                t += 0.6
            }
            let pattern = try CHHapticPattern(events: events, parameters: [])
            let playerNode = try engine.makePlayer(with: pattern)
            try playerNode.start(atTime: 0)
            // Re-trigger the burst so it keeps buzzing until acknowledged.
            hapticTimer = Timer.scheduledTimer(withTimeInterval: t, repeats: true) { [weak self] _ in
                Task { @MainActor in self?.playHapticBurst() }
            }
        } catch {
            startFallbackHaptics()
        }
    }

    private func startFallbackHaptics() {
        hapticTimer?.invalidate()
        hapticTimer = Timer.scheduledTimer(withTimeInterval: 0.9, repeats: true) { _ in
            UINotificationFeedbackGenerator().notificationOccurred(.warning)
        }
    }

    private func stopHaptics() {
        hapticTimer?.invalidate()
        hapticTimer = nil
        hapticEngine?.stop()
        hapticEngine = nil
    }
}
