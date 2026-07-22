import SwiftUI

@MainActor
final class InboxViewModel: ObservableObject {
    @Published var items: [NotificationItem] = []
    @Published var isLoading = false
    @Published var loadError: String?

    private var observer: NSObjectProtocol?

    init() {
        // Reload whenever something (a push, or the foreground poll) signals a refresh.
        observer = NotificationCenter.default.addObserver(
            forName: .smartEstateRefresh, object: nil, queue: .main) { [weak self] _ in
                Task { @MainActor in await self?.load() }
            }
    }

    deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    func load() async {
        guard !isLoading else { return } // avoid overlapping reloads from timer/task/observer
        isLoading = true
        loadError = nil
        defer { isLoading = false }
        do {
            items = try await APIClient.shared.notifications()
            NotificationSurfacer.shared.surface(items) // raise a local banner for any new notice
        } catch {
            loadError = LocalizationManager.shared.t("inbox.loadError")
        }
    }

    func open(_ item: NotificationItem) async {
        await APIClient.shared.markRead(id: item.id)
        await load()
    }
}

struct InboxView: View {
    @StateObject private var model = InboxViewModel()
    @EnvironmentObject var loc: LocalizationManager

    var body: some View {
        NavigationStack {
            List {
                if let err = model.loadError {
                    Text(err).foregroundStyle(.secondary)
                }
                ForEach(model.items) { item in
                    NotificationRow(item: item)
                        .contentShape(Rectangle())
                        .onTapGesture { Task { await model.open(item) } }
                }
            }
            .overlay {
                if model.items.isEmpty && !model.isLoading && model.loadError == nil {
                    ContentUnavailableView(loc.t("inbox.emptyTitle"), systemImage: "bell.slash",
                                           description: Text(loc.t("inbox.emptyDesc")))
                }
            }
            .navigationTitle(loc.t("inbox.title"))
            .refreshable { await model.load() }
            .task { await model.load() }
        }
    }
}
