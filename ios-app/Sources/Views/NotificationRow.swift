import SwiftUI

struct NotificationRow: View {
    let item: NotificationItem
    @EnvironmentObject var loc: LocalizationManager

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: item.isCritical ? "flame.fill" : "bell.fill")
                .foregroundStyle(item.isCritical ? .red : Color.accentColor)
                .font(.title3)
                .frame(width: 26)

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.headline)
                    .foregroundStyle(item.isCritical ? .red : .primary)
                Text(item.body)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
                HStack(spacing: 8) {
                    if let c = item.category { TagChip(text: loc.enumLabel(c)) }
                    if let s = item.severity, s != "INFO" { TagChip(text: loc.enumLabel(s)) }
                    Spacer()
                    if !item.read {
                        Circle().fill(.blue).frame(width: 8, height: 8)
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private struct TagChip: View {
    let text: String
    var body: some View {
        // `text` is already a display label (see loc.enumLabel), so render it verbatim.
        Text(text)
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(.quaternary, in: Capsule())
    }
}
