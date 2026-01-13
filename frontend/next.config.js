/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  basePath: '/aiprompt-tracker',
  assetPrefix: '/aiprompt-tracker/',
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
  // Note: rewrites() is ignored in 'export' mode
  // API calls will be made to same-origin /aiprompt-tracker/api/* when embedded
  // or to NEXT_PUBLIC_API_BASE_URL when deployed separately
};

module.exports = nextConfig;
