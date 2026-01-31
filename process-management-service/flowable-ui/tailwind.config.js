/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/**/*.{js,jsx,ts,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                primary: {
                    DEFAULT: '#3b82f6', // blue-500
                    soft: '#eff6ff',    // blue-50
                    dark: '#1d4ed8',    // blue-700
                },
                surface: {
                    DEFAULT: '#ffffff',
                    highlight: '#f8fafc', // slate-50
                },
                text: {
                    main: '#1e293b',      // slate-800
                    secondary: '#64748b', // slate-500
                    light: '#94a3b8',     // slate-400
                },
                border: {
                    light: '#e2e8f0',     // slate-200
                    DEFAULT: '#cbd5e1',   // slate-300
                },
                danger: '#ef4444',      // red-500
                success: '#10b981',     // emerald-500
                warning: '#f59e0b',     // amber-500
                'pastel-green': '#ecfdf5', // emerald-50
                'pastel-orange': '#fff7ed', // orange-50
            },
            fontFamily: {
                'display': ['Manrope', 'sans-serif'],
                'body': ['Inter', 'sans-serif'],
            },
            boxShadow: {
                'soft': '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)',
                'card': '0 0 0 1px rgba(226, 232, 240, 0.5), 0 2px 4px rgba(0, 0, 0, 0.05)',
                'float': '0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -2px rgba(0, 0, 0, 0.04)',
            }
        },
    },
    plugins: [],
}
