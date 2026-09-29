# Snip — URL Shortener

A Java Spring Boot and React application using SQLite to store shortened URLs.

## Requirements addressed

- **Fix deletion:** delete the requested alias and report whether it existed.
- **Generate aliases:** create seven-character alphanumeric aliases, retrying collisions up to ten total attempts.
- **Display URLs:** show short links, original URLs, and delete buttons in the frontend.
- **Add pagination:** return page metadata from the backend and provide Previous/Next controls in the UI.
- **Sort by creation date:** order by `created_at DESC, id DESC`, subject to the timestamp limitation below.
- **Docker setup:** connect the frontend and backend through Compose and persist SQLite in a named volume.
- **Documentation:** replace the original assessment README with this summary and the updated API contract.

## Problems found and fixed

- The original delete implementation created and removed a temporary record, leaving the requested URL untouched. It now deletes the requested alias directly.
- The creation endpoint returned an empty response without calling the service. It now saves the URL and returns its details with `201 Created`.
- Alias generation was a placeholder. Generated aliases now retry when insertion detects a duplicate, while duplicate custom aliases return an error. Database uniqueness handles collisions without relying on a separate existence check.
- Custom aliases now use an explicit ASCII format and reject reserved route names: `urls`, `shorten`, and `error`.
- The URL table was a placeholder, and the frontend expected an unpaginated array. The table and API client now consume the paginated response.
- Failed creation no longer clears the form. Deleting the latest created URL clears its result banner. Both successful creation and deletion reload the first page.
- The redirect test's name and expected status disagreed with the endpoint. Both now reflect `302 Found`.

## Known limitations and verification

- **Timestamp precision:** timestamps retain the existing ISO text format. Sorting can misorder values within the same second when fractional precision differs. This edge case was deliberately left unresolved.
- **Concurrent changes:** page contents and totals use separate queries, so they may briefly disagree if another request changes the data between queries.
- **Frontend testing:** automated frontend test files were removed; verification was manual.

The backend build and tests completed successfully with `mvn install`. Creation, listing, redirects, duplicate custom aliases, pagination, deletion, and persistence across Compose shutdown/startup were manually verified.

## Run

With Docker running, execute from the repository root:

```sh
docker compose up --build -d
```

Open [http://localhost:3000](http://localhost:3000). A new database volume starts empty; saved URLs persist in the named volume.

## Updated API contract

All endpoints are available through `http://localhost:3000` when using Compose.

| Method | Endpoint | Responses |
| --- | --- | --- |
| `POST` | `/shorten` | `201` with URL details; `400` for invalid input or a duplicate custom alias; `500` if generated-alias attempts are exhausted. |
| `GET` | `/urls?page=0&size=10` | `200` with a page of URLs; `400` for invalid parameters. |
| `GET` | `/{alias}` | `302` with the destination in the `Location` header; `404` if missing. |
| `DELETE` | `/{alias}` | `204` with no body; `404` if missing. |

### Create a URL

`POST /shorten`, with `Content-Type: application/json`:

```json
{
  "fullUrl": "https://example.com/long/path",
  "customAlias": "my-alias"
}
```

The destination must be an absolute HTTP or HTTPS URL. `customAlias` is optional; missing or blank values trigger generation. Custom aliases are trimmed, case-sensitive, and limited to 2–64 ASCII letters, digits, or hyphens. The names `urls`, `shorten`, and `error` are reserved.

Response (`201`):

```json
{
  "shortUrl": "http://localhost:3000/my-alias",
  "alias": "my-alias",
  "fullUrl": "https://example.com/long/path"
}
```

Validation and alias errors return an error message, for example:

```json
{
  "error": "The alias 'my-alias' is already taken."
}
```

### List URLs

`GET /urls?page=0&size=10`

`page` starts at zero and defaults to `0`. `size` defaults to `10` and must be between `1` and `100`.

The response is now a page object rather than an array (`200`):

```json
{
  "items": [
    {
      "alias": "my-alias",
      "fullUrl": "https://example.com/long/path",
      "shortUrl": "http://localhost:3000/my-alias"
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 1,
  "totalPages": 1
}
```

Pages beyond the available records return empty `items` while preserving the requested page and database totals. An empty database has zero total items and pages.
