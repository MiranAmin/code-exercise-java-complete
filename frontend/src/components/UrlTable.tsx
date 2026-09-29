import type { UrlListItem } from '../types/api';

interface UrlTableProps {
  urls: UrlListItem[];
  onDelete: (alias: string) => void;
}

export function UrlTable({ urls, onDelete }: UrlTableProps) {
  if (urls.length === 0) {
    return (
      <div className="empty-state">
        <p>No shortened URLs yet.</p>
      </div>
    );
  }

  return (
    <div className="url-table-wrapper">
      <table className="url-table" aria-label="Shortened URLs">
        <thead>
          <tr>
            <th scope="col">Short URL</th>
            <th scope="col">Original URL</th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          {urls.map((url) => (
            <tr key={url.alias}>
              <td>
                <a className="short-url-link" href={url.shortUrl} target="_blank" rel="noopener noreferrer">
                  {url.shortUrl}
                </a>
              </td>
              <td>
                <span className="full-url" title={url.fullUrl}>{url.fullUrl}</span>
              </td>
              <td className="actions-cell">
                <button type="button" className="btn-danger" aria-label={`Delete ${url.alias}`} onClick={() => onDelete(url.alias)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
