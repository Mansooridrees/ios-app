import Foundation

public class BackendService: ObservableObject {
    @Published public var baseUrl: String = "http://localhost:3000"
    @Published public var isConnected: Bool = false

    public static let shared = BackendService()

    public init() {}

    public func checkHealth(completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "\(baseUrl)/health") else {
            completion(false)
            return
        }

        URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                let success = error == nil && (response as? HTTPURLResponse)?.statusCode == 200
                self.isConnected = success
                completion(success)
            }
        }.resume()
    }

    public func fetchInvoices(completion: @escaping ([Invoice]?) -> Void) {
        guard let url = URL(string: "\(baseUrl)/api/invoices") else {
            completion(nil)
            return
        }

        URLSession.shared.dataTask(with: url) { data, response, error in
            guard let data = data, error == nil else {
                DispatchQueue.main.async { completion(nil) }
                return
            }

            struct InvoicesResponse: Codable {
                let invoices: [Invoice]
            }

            let result = try? JSONDecoder().decode(InvoicesResponse.self, from: data)
            DispatchQueue.main.async {
                completion(result?.invoices)
            }
        }.resume()
    }

    public func fetchMetrics(completion: @escaping (LedgerMetrics?) -> Void) {
        guard let url = URL(string: "\(baseUrl)/api/metrics") else {
            completion(nil)
            return
        }

        URLSession.shared.dataTask(with: url) { data, response, error in
            guard let data = data, error == nil else {
                DispatchQueue.main.async { completion(nil) }
                return
            }

            struct MetricsResponse: Codable {
                let metrics: LedgerMetrics
            }

            let result = try? JSONDecoder().decode(MetricsResponse.self, from: data)
            DispatchQueue.main.async {
                completion(result?.metrics)
            }
        }.resume()
    }

    public func saveInvoice(_ invoice: Invoice, completion: @escaping (Invoice?) -> Void) {
        guard let url = URL(string: "\(baseUrl)/api/invoices") else {
            completion(nil)
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        do {
            request.httpBody = try JSONEncoder().encode(invoice)
        } catch {
            completion(nil)
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, error == nil else {
                DispatchQueue.main.async { completion(nil) }
                return
            }

            struct SaveResponse: Codable {
                let invoice: Invoice?
            }

            let result = try? JSONDecoder().decode(SaveResponse.self, from: data)
            DispatchQueue.main.async {
                completion(result?.invoice ?? invoice)
            }
        }.resume()
    }
}

