import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { Calendar, ShieldCheck } from 'lucide-react';

export const Layout: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 transition-colors dark:bg-[#0b1120] dark:text-slate-100">
      <Navbar />
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white py-6 text-sm text-slate-500 transition-colors dark:border-slate-800 dark:bg-slate-900 dark:text-slate-400">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 font-medium text-slate-700 dark:text-slate-300">
            <Calendar className="h-4 w-4 text-brand-600 dark:text-brand-400" />
            <span>APPOINTENSE &copy; {new Date().getFullYear()}</span>
            <span className="text-slate-300 dark:text-slate-700">|</span>
            <span className="text-xs text-slate-500 dark:text-slate-400 font-normal">Online Appointment Booking System</span>
          </div>
          <div className="flex items-center gap-4 text-xs">
            <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
              <ShieldCheck className="h-3.5 w-3.5" />
              Spring Security + JWT Protected
            </span>
            <span className="text-slate-300 dark:text-slate-700">•</span>
            <span>REST API & MySQL</span>
          </div>
        </div>
      </footer>
    </div>
  );
};
