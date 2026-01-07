/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  basePath: '/aiprompt-tracker',
  assetPrefix: '/aiprompt-tracker/',
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'}/aiprompt-tracker/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
