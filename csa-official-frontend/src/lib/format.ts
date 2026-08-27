const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "待补充"
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace("T", " ")
  }

  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date)
}

export function excerptText(value?: string | null, maxLength = 140) {
  if (!value) {
    return ""
  }

  const plainText = value
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim()

  if (plainText.length <= maxLength) {
    return plainText
  }

  return `${plainText.slice(0, maxLength).trimEnd()}...`
}

export function formatBalance(value?: number | string | null) {
  if (value == null || value === "") {
    return "--"
  }

  const numericValue = typeof value === "number" ? value : Number(value)

  if (Number.isNaN(numericValue)) {
    return String(value)
  }

  return `¥${numericValue.toFixed(2)}`
}

export function parseVoteResult(result?: string | null) {
  if (!result) {
    return null
  }

  try {
    const parsed = JSON.parse(result) as { agree?: number; reject?: number }
    return {
      agree: parsed.agree ?? 0,
      reject: parsed.reject ?? 0,
    }
  } catch {
    return null
  }
}

export function resolveAssetUrl(value?: string | null) {
  if (!value) {
    return ""
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  if (value.startsWith("//")) {
    return `https:${value}`
  }

  const normalizedBaseUrl = apiBaseUrl.replace(/\/$/, "")
  const normalizedPath = value.startsWith("/") ? value : `/${value}`
  return `${normalizedBaseUrl}${normalizedPath}`
}
