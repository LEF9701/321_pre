import { useMemo, useState } from 'react';

/**
 * Client-side search + sort for the live record tables (sensors, access log). The data is bounded
 * (a page of rows) and updates via SSE, so filtering/sorting in the browser stays responsive and
 * keeps working as new rows arrive.
 *
 *   const ctl = useTableControls(rows, {
 *     searchText: (r) => `${r.code} ${r.location}`,   // what a search query matches against
 *     initialSort: 'at', initialDir: 'desc',
 *   });
 *   ctl.view            // filtered + sorted rows to render
 *   <SearchBox value={ctl.search} onChange={ctl.setSearch} />
 *   <SortTh label="Time" k="at" ctl={ctl} />
 */
export function useTableControls(rows, { searchText, initialSort = null, initialDir = 'desc', sortValue } = {}) {
  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState(initialSort);
  const [sortDir, setSortDir] = useState(initialDir);

  function toggleSort(key) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  }

  const view = useMemo(() => {
    let out = rows || [];
    const q = search.trim().toLowerCase();
    if (q && searchText) {
      out = out.filter((r) => String(searchText(r) || '').toLowerCase().includes(q));
    }
    if (sortKey) {
      const val = sortValue || ((r, k) => r[k]);
      out = [...out].sort((a, b) => {
        const av = val(a, sortKey);
        const bv = val(b, sortKey);
        let c;
        if (typeof av === 'number' && typeof bv === 'number') c = av - bv;
        else c = String(av ?? '').localeCompare(String(bv ?? ''));
        return sortDir === 'asc' ? c : -c;
      });
    }
    return out;
  }, [rows, search, sortKey, sortDir, searchText, sortValue]);

  return { view, search, setSearch, sortKey, sortDir, toggleSort };
}
