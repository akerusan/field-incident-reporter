"use client";

/** Shown when the API can't be reached or answers with an error. */
export default function ErrorPage({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-start gap-3 rounded-lg border border-red-300 p-6 dark:border-red-800">
      <h1 className="text-lg font-semibold">Something went wrong</h1>
      <p className="text-sm text-slate-700 dark:text-slate-300">{error.message}</p>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        Is the mock API running? Start it with <code className="font-mono">npm start</code> in{" "}
        <code className="font-mono">server/</code>.
      </p>
      <button
        type="button"
        onClick={reset}
        className="rounded-md bg-blue-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-800"
      >
        Try again
      </button>
    </div>
  );
}
