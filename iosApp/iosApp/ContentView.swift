import SwiftUI
import shared

struct ContentView: View {
  @ObservedObject private(set) var viewModel: ViewModel

    var body: some View {
        NavigationView {
            listView()
            .navigationBarTitle("SpaceX Launches")
            .navigationBarItems(trailing:
                Button("Reload") {
                    self.viewModel.fetchDataFromInternet()
            })
        }
    }

    private func listView() -> AnyView {
        switch viewModel.launches {
        case .loading:
            return AnyView(Text("Loading...").multilineTextAlignment(.center))
        case .result(let launches):
            return AnyView(List(launches) { launch in
                
                RocketLaunchRow(rocketLaunch: launch, onClickCloseIcon: { item in
                    self.viewModel.removeLaunch(launch: item)
                })
                
//                RocketLaunchRow(rocketLaunch: launch, onClickCloseIcon: {
//
//                } ) { rocketLaunch in
//                    print(rocketLaunch)
//                }
                
            })
        case .error(let description):
            return AnyView(Text(description).multilineTextAlignment(.center))
        }
    }
}


extension ContentView {

    enum LoadableLaunches {
        case loading
        case result([RocketLaunch])
        case error(String)
    }

    class ViewModel: ObservableObject {
        let sdk: SpaceXSDK
        @Published var launches = LoadableLaunches.loading

        init(sdk: SpaceXSDK) {
            print("init")
            self.sdk = sdk
            self.observeData()
            self.fetchDataFromInternet()
        }
        
        func observeData() {
            print("observerData()")
            do {
                try sdk.observeAllLaunchesFromDb().watch { data in
                    let launches = data as! [RocketLaunch]
                    print("WATCH data, size = ", launches.count)
                    self.launches = .result(launches)
                }
            } catch {
                print(error)
            }
        }

        func loadLaunches(forceReload: Bool) {
            self.launches = .loading
            sdk.getLaunches(forceReload: forceReload, completionHandler: { launches, error in
                if let launches = launches {
                    self.launches = .result(launches)
                } else {
                    self.launches = .error(error?.localizedDescription ?? "error")
                }
            })
        }
        
        func fetchDataFromInternet() {
            print("fetchDataFromInternet()")
            self.launches = .loading
            sdk.fetchLaunchesFromInternet(completionHandler: { launches, error in
            })
        }
        
        func removeLaunch(launch: RocketLaunch) {
            do {
                try sdk.removeLaunch(launch: launch)
            } catch {
                print(error)
            }
        }
    }
}

extension RocketLaunch: Identifiable { }
