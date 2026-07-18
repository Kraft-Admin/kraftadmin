import { writable } from 'svelte/store';

export const adminSettings = writable({
  basePath: '/admin',
  title: 'KraftAdmin',
  logoUrl: '',
  version: '0.0.1',
  theme: {
    primaryColor: '#3b82f6',
    darkMode: true
  },
  storage: {
    uploadDir: 'uploads/admin',
    publicUrlPrefix: '/admin/files'
  },
  // Read-only display info — never sent back in an update request.
  security: {
    cookieName: 'KRAFT_SESSION',
    sessionExpiryMinutes: 60
  },
  pagination: {
    defaultPageSize: 20,
    maxPageSize: 100
  },
  features: {
    allowDelete: true,
    showTimestamps: true,
    readOnly: false
  },
  localeConfig: {
    defaultLanguage: 'en',
    timezone: 'UTC'
  },
  telemetryConfig: {
    cloudUrl: 'http://localhost:8090',
    enabled: true
  }
});