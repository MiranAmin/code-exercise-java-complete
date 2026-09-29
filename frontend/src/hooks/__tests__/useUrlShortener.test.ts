import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useUrlShortener } from '../useUrlShortener';
import { api } from '../../services/api';

vi.mock('../../services/api', () => ({
  api: {
    listAll: vi.fn(),
    shorten: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('useUrlShortener', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(api.listAll).mockResolvedValue({
      items: [],
      page: 0,
      size: 10,
      totalItems: 0,
      totalPages: 1,
    });
  });

  it('loads URLs and can change page', async () => {
    const { result } = renderHook(() => useUrlShortener());

    await waitFor(() => {
      expect(api.listAll).toHaveBeenCalledWith(0, 10);
    });

    await act(async () => {
      await result.current.goToPage(1);
    });

    expect(api.listAll).toHaveBeenCalledWith(1, 10);
  });

  it('creates a URL', async () => {
    vi.mocked(api.shorten).mockResolvedValue({
      alias: 'test',
      fullUrl: 'https://example.com',
      shortUrl: 'http://localhost:8080/test',
    });

    const { result } = renderHook(() => useUrlShortener());

    await waitFor(() => {
      expect(api.listAll).toHaveBeenCalled();
    });

    await act(async () => {
      await result.current.shorten('https://example.com', 'test');
    });

    expect(result.current.lastCreated?.alias).toBe('test');
  });

  it('clears the created URL after deleting it', async () => {
    vi.mocked(api.shorten).mockResolvedValue({
      alias: 'test',
      fullUrl: 'https://example.com',
      shortUrl: 'http://localhost:8080/test',
    });

    vi.mocked(api.delete).mockResolvedValue();

    const { result } = renderHook(() => useUrlShortener());

    await waitFor(() => {
      expect(api.listAll).toHaveBeenCalled();
    });

    await act(async () => {
      await result.current.shorten('https://example.com', 'test');
      await result.current.deleteUrl('test');
    });

    expect(result.current.lastCreated).toBeNull();
  });
});