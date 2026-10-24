import React from 'react';
import { Link } from 'react-router-dom';
import { 
  Calendar, 
  Clock, 
  ShieldCheck, 
  Layers, 
  ArrowRight, 
  Lock 
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const HomePage: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="space-y-16 py-6">
      {/* Hero Section */}
      <div className="text-center max-w-3xl mx-auto space-y-6">
        <div className="inline-flex items-center gap-2 rounded-full border border-brand-200 bg-brand-50 px-3.5 py-1 text-xs font-semibold text-brand-700 shadow-sm dark:border-brand-800 dark:bg-brand-950/60 dark:text-brand-300">
          <Calendar className="h-3.5 w-3.5" />
          <span>Spring Boot 3 + MySQL + React Architecture</span>
        </div>

        <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-5xl md:text-6xl">
          Online Appointment Booking <br className="hidden sm:inline" />
          <span className="text-brand-600 dark:text-brand-400">Simplified & Secured</span>
        </h1>

        <p className="text-lg text-slate-600 dark:text-slate-300 leading-relaxed max-w-2xl mx-auto">
          Experience reliable, conflict-free scheduling with instant slot verification, role-based security, and live booking updates.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
          {isAuthenticated ? (
            <Link
              to="/dashboard"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 px-6 py-3 text-base font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 transition-colors"
            >
              <span>Go to Your Dashboard</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
          ) : (
            <>
              <Link
                to="/register"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 px-6 py-3 text-base font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 transition-colors"
              >
                <span>Get Started Now</span>
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                to="/slots"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-6 py-3 text-base font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 transition-colors"
              >
                <Clock className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                <span>Browse Available Slots</span>
              </Link>
            </>
          )}
        </div>
      </div>

      {/* Feature Highlights Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-3 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-950/60 dark:text-brand-400">
            <Lock className="h-6 w-6" />
          </div>
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">Zero Overbooking</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
            Database-enforced unique constraints and atomic slot transitions guarantee that no two users can book the same slot at the same time.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-3 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">Spring Security + JWT</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
            Stateless authentication powered by cryptographic tokens with BCrypt password hashing and strict role-based authorization for USER and ADMIN.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-3 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-50 text-purple-600 dark:bg-purple-950/60 dark:text-purple-400">
            <Layers className="h-6 w-6" />
          </div>
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">Layered Architecture</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
            Strict separation of concerns between REST controllers, business services, domain repositories, and standard HTTP error handling.
          </p>
        </div>
      </div>

      {/* Quick Status / College Project Banner */}
      <div className="rounded-2xl bg-slate-900 text-white p-8 shadow-sm flex flex-col md:flex-row items-center justify-between gap-6 dark:bg-slate-800 dark:border dark:border-slate-700">
        <div className="space-y-1 text-center md:text-left">
          <h3 className="text-xl font-bold">College Mini Project Architecture Demonstration</h3>
          <p className="text-sm text-slate-400">
            Stack: Java 21, Spring Boot 3.4.3, Spring Data JPA, MySQL 8.0, React 19, TypeScript, Vite & Tailwind CSS.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link
            to="/services"
            className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-500 transition-colors dark:bg-brand-500 dark:hover:bg-brand-600"
          >
            Explore Services
          </Link>
        </div>
      </div>
    </div>
  );
};
