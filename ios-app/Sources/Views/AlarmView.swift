import SwiftUI

/// Full-screen, hard-to-miss fire alarm. Flashes red, shows the message, and only dismisses when
/// the resident taps ACKNOWLEDGE (which stops the siren/haptics and marks the alert read).
struct AlarmView: View {
    @EnvironmentObject var alarm: AlarmController
    @EnvironmentObject var loc: LocalizationManager
    @State private var flash = false

    var body: some View {
        ZStack {
            (flash ? Color.red : Color(red: 0.55, green: 0, blue: 0))
                .ignoresSafeArea()
                .animation(.easeInOut(duration: 0.5).repeatForever(autoreverses: true), value: flash)

            VStack(spacing: 22) {
                Image(systemName: "flame.fill")
                    .font(.system(size: 84))
                    .foregroundStyle(.white)

                Text(alarm.title)
                    .font(.largeTitle).bold()
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white)

                Text(alarm.body)
                    .font(.title3)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white.opacity(0.95))
                    .padding(.horizontal)

                Spacer()

                Button {
                    alarm.acknowledge()
                } label: {
                    Text(loc.t("alarm.acknowledge"))
                        .font(.title2).bold()
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(.white, in: RoundedRectangle(cornerRadius: 16))
                        .foregroundStyle(.red)
                }
                .padding(.horizontal, 32)
            }
            .padding(.vertical, 64)
        }
        .onAppear { flash = true }
    }
}
