import Link from "next/link";
import {
  CATEGORIES,
  CATEGORY_LABELS,
  SEVERITIES,
  SEVERITY_LABELS,
  STATUSES,
  STATUS_LABELS,
  type IncidentView,
} from "@/lib/incidents";

const selectStyle =
  "rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm dark:border-slate-600 dark:bg-slate-800";

/**
 * Filters as a plain GET form: the view lives in the URL, works without JavaScript, and a filtered
 * list can be bookmarked or shared.
 */
export function FilterBar({ view }: { view: IncidentView }) {
  return (
    <form method="get" className="flex flex-wrap items-end gap-3" aria-label="Filter incidents">
      <Select name="status" label="Status" value={view.status} options={STATUSES} labels={STATUS_LABELS} />
      <Select name="severity" label="Severity" value={view.severity} options={SEVERITIES} labels={SEVERITY_LABELS} />
      <Select name="category" label="Category" value={view.category} options={CATEGORIES} labels={CATEGORY_LABELS} />
      <label className="flex flex-col gap-1 text-xs font-medium text-slate-600 dark:text-slate-300">
        Sort
        <select name="sort" defaultValue={view.sort} className={selectStyle}>
          <option value="newest">Newest first</option>
          <option value="severity">Most severe first</option>
        </select>
      </label>
      <button
        type="submit"
        className="rounded-md bg-blue-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-800 dark:bg-blue-600 dark:hover:bg-blue-500"
      >
        Apply
      </button>
      <Link href="/" className="px-1 py-1.5 text-sm text-slate-600 underline-offset-2 hover:underline dark:text-slate-300">
        Clear
      </Link>
    </form>
  );
}

function Select<T extends string>(props: {
  name: string;
  label: string;
  value: T | undefined;
  options: readonly T[];
  labels: Record<T, string>;
}) {
  return (
    <label className="flex flex-col gap-1 text-xs font-medium text-slate-600 dark:text-slate-300">
      {props.label}
      <select name={props.name} defaultValue={props.value ?? ""} className={selectStyle}>
        <option value="">All</option>
        {props.options.map((option) => (
          <option key={option} value={option}>
            {props.labels[option]}
          </option>
        ))}
      </select>
    </label>
  );
}
