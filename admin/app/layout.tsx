import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'OTT Platform Admin Dashboard',
  description: 'Admin panel for managing episodes, streaming links, and viewer monitoring',
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet" />
      </head>
      <body className="bg-gray-900 text-gray-100 font-inter antialiased">
        {children}
      </body>
    </html>
  );
}