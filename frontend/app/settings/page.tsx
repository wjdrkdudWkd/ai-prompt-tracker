import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

export default function SettingsPage() {
  return (
    <div className="flex flex-col gap-6 p-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
        <p className="text-muted-foreground">Configure AI Prompt Tracker preferences</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Settings</CardTitle>
          <CardDescription>Configuration options coming soon</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">Settings functionality will be implemented in a future update.</p>
        </CardContent>
      </Card>
    </div>
  );
}
