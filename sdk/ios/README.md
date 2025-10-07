iOS SDK snippet (Swift)

```
let url = URL(string: "https://your.host/api/v1/chat")!
var req = URLRequest(url: url)
req.httpMethod = "POST"
req.addValue("application/json", forHTTPHeaderField: "Content-Type")
let payload: [String: Any] = ["tenantId": "acme", "question": "Hello", "allowWeb": false]
req.httpBody = try! JSONSerialization.data(withJSONObject: payload)
// req.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
let task = URLSession.shared.dataTask(with: req){ data, resp, err in
    if let d = data { print(String(data: d, encoding: .utf8) ?? "") }
}
task.resume()
```
