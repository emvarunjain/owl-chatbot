Android SDK snippet (Kotlin)

Example using OkHttp:

```
val client = OkHttpClient()
val body = """
{"tenantId":"acme","question":"Hello","allowWeb":false}
""".trimIndent().toRequestBody("application/json".toMediaType())
val req = Request.Builder().url("https://your.host/api/v1/chat").post(body).build()
client.newCall(req).execute().use { resp -> println(resp.body?.string()) }
```

Add auth header if using JWT:

```
.header("Authorization", "Bearer $token")
```
