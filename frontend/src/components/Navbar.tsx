import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ThemeToggle } from './ThemeToggle';
import { 
  Calendar, 
  Layers, 
  Clock, 
  LogOut, 
  Shield, 
  Menu, 
  X,
  Activity
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `inline-flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
      isActive 
        ? 'bg-brand-50 text-brand-700 font-semibold dark:bg-brand-950/60 dark:text-brand-300' 
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100 dark:text-slate-300 dark:hover:text-white dark:hover:bg-slate-800'
    }`;

  const mobileNavLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3 py-2.5 text-base font-medium rounded-md ${
      isActive 
        ? 'bg-brand-50 text-brand-700 font-semibold dark:bg-brand-950/60 dark:text-brand-300' 
        : 'text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
    }`;

  return (
    <nav className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur transition-colors dark:border-slate-800 dark:bg-slate-900/95">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-8">
            <Link to="/" className="flex items-center gap-2.5 focus:outline-none focus:ring-2 focus:ring-brand-500 rounded-md p-1">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-brand-600 to-brand-400 text-white shadow-sm">
                <Calendar className="h-5 w-5" />
              </div>
              <div className="flex flex-col">
                <span className="text-lg font-bold tracking-tight text-slate-900 dark:text-white">APPOINTENSE</span>
                <span className="text-[10px] uppercase tracking-wider font-semibold text-brand-600 dark:text-brand-400">Smart Booking</span>
              </div>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden md:flex md:items-center md:gap-1">
              <NavLink to="/services" className={navLinkClass}>
                <Layers className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                Services
              </NavLink>
              <NavLink to="/slots" className={navLinkClass}>
                <Clock className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                Available Slots
              </NavLink>

              {isAuthenticated && (
                <>
                  <NavLink to="/dashboard" className={navLinkClass}>
                    <Activity className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                    Dashboard
                  </NavLink>
                  <NavLink to="/my-appointments" className={navLinkClass}>
                    <Calendar className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                    My Appointments
                  </NavLink>
                </>
              )}

              {isAdmin && (
                <NavLink to="/admin" className={navLinkClass}>
                  <Shield className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                  <span className="text-amber-700 dark:text-amber-300 font-medium">Admin Portal</span>
                </NavLink>
              )}
            </div>
          </div>

          {/* User actions / Theme Toggle / Auth Buttons */}
          <div className="hidden md:flex md:items-center md:gap-3">
            <ThemeToggle />

            {isAuthenticated ? (
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 dark:border-slate-800 dark:bg-slate-800/80">
                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-100 text-brand-700 font-semibold text-xs dark:bg-brand-900 dark:text-brand-300">
                    {user?.fullName?.charAt(0) || 'U'}
                  </div>
                  <div className="flex flex-col text-left">
                    <span className="text-xs font-semibold text-slate-900 dark:text-white leading-tight">{user?.fullName}</span>
                    <span className="text-[10px] font-medium text-slate-500 dark:text-slate-400">{user?.role}</span>
                  </div>
                </div>
                <button
                  onClick={handleLogout}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 shadow-sm hover:bg-slate-50 hover:text-red-600 focus:outline-none focus:ring-2 focus:ring-brand-500 transition-colors dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-red-400"
                  aria-label="Log out"
                >
                  <LogOut className="h-3.5 w-3.5" />
                  Logout
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="rounded-lg px-3.5 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="inline-flex items-center justify-center rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 transition-colors dark:bg-brand-500 dark:hover:bg-brand-600"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>

          {/* Mobile hamburger & Theme Toggle */}
          <div className="flex items-center gap-2 md:hidden">
            <ThemeToggle />
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="inline-flex items-center justify-center rounded-md p-2 text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white focus:outline-none focus:ring-2 focus:ring-brand-500"
              aria-label="Toggle navigation menu"
            >
              {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && (
        <div className="border-b border-slate-200 bg-white px-4 pt-2 pb-4 dark:border-slate-800 dark:bg-slate-900 md:hidden space-y-1">
          <NavLink
            to="/services"
            className={mobileNavLinkClass}
            onClick={() => setMobileMenuOpen(false)}
          >
            <Layers className="h-5 w-5 text-slate-500 dark:text-slate-400" />
            Services
          </NavLink>
          <NavLink
            to="/slots"
            className={mobileNavLinkClass}
            onClick={() => setMobileMenuOpen(false)}
          >
            <Clock className="h-5 w-5 text-slate-500 dark:text-slate-400" />
            Available Slots
          </NavLink>

          {isAuthenticated && (
            <>
              <NavLink
                to="/dashboard"
                className={mobileNavLinkClass}
                onClick={() => setMobileMenuOpen(false)}
              >
                <Activity className="h-5 w-5 text-slate-500 dark:text-slate-400" />
                Dashboard
              </NavLink>
              <NavLink
                to="/my-appointments"
                className={mobileNavLinkClass}
                onClick={() => setMobileMenuOpen(false)}
              >
                <Calendar className="h-5 w-5 text-slate-500 dark:text-slate-400" />
                My Appointments
              </NavLink>
            </>
          )}

          {isAdmin && (
            <NavLink
              to="/admin"
              className={mobileNavLinkClass}
              onClick={() => setMobileMenuOpen(false)}
            >
              <Shield className="h-5 w-5 text-amber-600 dark:text-amber-400" />
              Admin Portal
            </NavLink>
          )}

          <div className="pt-3 border-t border-slate-200 dark:border-slate-800">
            {isAuthenticated ? (
              <div className="flex flex-col gap-2">
                <div className="px-3 py-1 text-xs text-slate-500 dark:text-slate-400">
                  Signed in as <strong className="text-slate-800 dark:text-slate-200">{user?.fullName}</strong> ({user?.role})
                </div>
                <button
                  onClick={() => {
                    setMobileMenuOpen(false);
                    handleLogout();
                  }}
                  className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/40"
                >
                  <LogOut className="h-4 w-4" />
                  Logout
                </button>
              </div>
            ) : (
              <div className="flex flex-col gap-2 pt-1">
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center rounded-lg border border-slate-300 dark:border-slate-700 py-2 text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center rounded-lg bg-brand-600 py-2 text-sm font-medium text-white hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600"
                >
                  Create Account
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </nav>
  );
};
