import { AccountSecurityPanel } from "@/components/business/settings/AccountSecurityPanel"
import { AboutEditor } from "@/components/business/settings/AboutEditor"

export default function DashboardSettingsPage() {
  return (
    <div className="space-y-10">
      <AccountSecurityPanel />
      <AboutEditor />
    </div>
  )
}
