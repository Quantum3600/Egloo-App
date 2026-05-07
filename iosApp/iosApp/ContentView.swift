import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onOpenURL { url in
                handleDeepLink(url)
            }
    }

    private func handleDeepLink(_ url: URL) {
        if url.scheme == "egloo" && url.host == "auth" {
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            let status = components?.queryItems?.first(where: { $0.name == "status" })?.value
            let source = components?.queryItems?.first(where: { $0.name == "source" })?.value

            if let status = status, let source = source {
                // Call Shared Kotlin code to emit the deep link result
                _ = DeepLinkHandler.shared.emitAuthResult(status: status, source: source) { _ in }
            }
        }
    }
}

