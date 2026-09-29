import { useState, useCallback, useEffect } from 'react';
import { api } from '../services/api';
import type { ShortenUrlResponse, UrlListItem } from '../types/api';

const PAGE_SIZE = 10;

interface UseUrlShortenerReturn {
  urls: UrlListItem[];
  loading: boolean;
  error: string | null;
  lastCreated: ShortenUrlResponse | null;
  page: number;
  totalItems: number;
  totalPages: number;
  goToPage: (page: number) => Promise<void>;
  shorten: (fullUrl: string, customAlias?: string) => Promise<boolean>;
  deleteUrl: (alias: string) => Promise<void>;
  clearLastCreated: () => void;
}

export function useUrlShortener(): UseUrlShortenerReturn {
  const [urls, setUrls] = useState<UrlListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastCreated, setLastCreated] = useState<ShortenUrlResponse | null>(null);
  const [page, setPage] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchPage = useCallback(async (requestedPage: number) => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.listAll(requestedPage, PAGE_SIZE);
      setUrls(data.items);
      setPage(data.page);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load URLs.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchPage(0);
  }, [fetchPage]);

  const shorten = useCallback(async (fullUrl: string, customAlias?: string) => {
    setError(null);
    setLastCreated(null);

    try {
      const result = await api.shorten({
        fullUrl,
        customAlias: customAlias || undefined,
      });

      setLastCreated(result);
      await fetchPage(0);
      return true;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to shorten URL.');
      return false;
    }
  }, [fetchPage]);

  const deleteUrl = useCallback(async (alias: string) => {
    setError(null);

    try {
      await api.delete(alias);

      setLastCreated((previous) =>
          previous?.alias === alias ? null : previous
      );

      await fetchPage(0);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete URL.');
    }
  }, [fetchPage]);

  const clearLastCreated = useCallback(() => setLastCreated(null), []);

  return {
    urls,
    loading,
    error,
    lastCreated,
    page,
    totalItems,
    totalPages,
    goToPage: fetchPage,
    shorten,
    deleteUrl,
    clearLastCreated,
  };
}
