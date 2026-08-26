import type { Metadata } from "next"
import { headers } from "next/headers"

import { AppProviders } from "@/components/providers/AppProviders"

import "./globals.css"

export const metadata: Metadata = {
  title: {
    default: "CSA 计算机协会",
    template: "%s | CSA 计算机协会",
  },
  description:
    "广州华立学院计算机协会的资源、比赛、简历与组织协作平台。",
  applicationName: "CSA Official",
}

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  // A nonce-based CSP requires a fresh HTML document for every request.
  const nonce = (await headers()).get("x-nonce") ?? undefined

  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body className="min-h-screen bg-background font-sans text-foreground antialiased">
        <AppProviders nonce={nonce}>{children}</AppProviders>
      </body>
    </html>
  )
}
