import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Field Incident Reporter · Operator dashboard",
  description: "Triage incidents reported from the field.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen">
        <header className="border-b border-slate-200 dark:border-slate-800">
          <div className="mx-auto flex max-w-6xl items-baseline gap-3 px-4 py-3">
            <Link href="/" className="text-lg font-semibold">
              Field Incident Reporter
            </Link>
            <span className="text-sm text-slate-600 dark:text-slate-400">Operator dashboard</span>
          </div>
        </header>
        <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
      </body>
    </html>
  );
}
