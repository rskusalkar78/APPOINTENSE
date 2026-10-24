import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { AppointmentItem } from '../types';
import { 
  Calendar, 
  Clock, 
  ArrowRight, 
  CheckCircle, 
  XCircle, 
  Sparkles, 
  Layers,
  ChevronRight,
  ShieldCheck
} from 'lucide-react';

export const UserDashboard: React.FC = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUserAppointments = async () => {
      if (!user) return;
      try {
        const res = await api.get<AppointmentItem[]>(`/appointments/user/${user.id}`);
        setAppointments(res.data);
      } catch (e) {
        console.error('Failed to load user appointments', e);
      } finally {
        setLoading(false);
      }
    };

    fetchUserAppointments();
  }, [user]);

  const upcomingAppointments = appointments
    .filter((a) => a.status === 'BOOKED' && new Date(a.startTime) >= new Date())
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());

  const pastAppointments = appointments
    .filter((a) => a.status === 'CANCELLED' || new Date(a.startTime) < new Date())
    .slice(0, 5);

  const formatDateTime = (isoString: string) => {
    const d = new Date(isoString);
    return {
      date: d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
  };

  return (
    <div className="space-y-8">
      {/* Welcome Hero Card */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-brand-700 via-brand-600 to-indigo-700 p-8 text-white shadow-md">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold backdrop-blur mb-4">
            <Sparkles className="h-3.5 w-3.5 text-amber-300" />
            <span>Authenticated as {user?.role}</span>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">
            Welcome back, {user?.fullName}!
          </h1>
          <p className="mt-2 text-brand-100 text-sm sm:text-base">
            Easily manage your active bookings, view schedule history, or explore available specialized consultation slots.
          </p>

          <div className="mt-6 flex flex-wrap gap-3">
            <Link
              to="/slots"
              className="inline-flex items-center gap-2 rounded-xl bg-white px-5 py-2.5 text-sm font-semibold text-brand-700 shadow-sm hover:bg-brand-50 transition-colors"
            >
              <span>Book an Appointment</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              to="/services"
              className="inline-flex items-center gap-2 rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur hover:bg-white/20 transition-colors"
            >
              <Layers className="h-4 w-4" />
              <span>Browse Services</span>
            </Link>
          </div>
        </div>

        <div className="absolute -right-8 -bottom-10 h-64 w-64 rounded-full bg-white/5 pointer-events-none" />
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Upcoming</span>
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400">
              <CheckCircle className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">{upcomingAppointments.length}</p>
          <span className="text-xs text-slate-500 dark:text-slate-400">Confirmed active bookings</span>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Total Bookings</span>
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-50 text-brand-600 dark:bg-brand-950/60 dark:text-brand-400">
              <Calendar className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">{appointments.length}</p>
          <span className="text-xs text-slate-500 dark:text-slate-400">Lifetime reservations</span>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Security</span>
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-purple-50 text-purple-600 dark:bg-purple-950/60 dark:text-purple-400">
              <ShieldCheck className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-sm font-bold text-slate-900 dark:text-white">BCrypt + HMAC-SHA256</p>
          <span className="text-xs text-slate-500 dark:text-slate-400">Verified session token</span>
        </div>
      </div>

      {/* Upcoming Appointments Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">Next Upcoming Appointments</h2>
          <Link to="/my-appointments" className="text-sm font-semibold text-brand-600 dark:text-brand-400 hover:underline flex items-center gap-1">
            <span>View all</span>
            <ChevronRight className="h-4 w-4" />
          </Link>
        </div>

        {loading ? (
          <div className="space-y-3">
            {[1, 2].map((n) => (
              <div key={n} className="h-24 rounded-xl border border-slate-200 bg-white p-5 animate-pulse dark:border-slate-800 dark:bg-slate-900" />
            ))}
          </div>
        ) : upcomingAppointments.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center dark:border-slate-800 dark:bg-slate-900">
            <Calendar className="mx-auto h-8 w-8 text-slate-400" />
            <p className="mt-2 text-sm font-medium text-slate-900 dark:text-white">No upcoming appointments</p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Explore available slots to schedule a consultation.</p>
            <Link
              to="/slots"
              className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-brand-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600"
            >
              Browse Open Slots
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {upcomingAppointments.map((appt) => {
              const start = formatDateTime(appt.startTime);
              const end = formatDateTime(appt.endTime);

              return (
                <div
                  key={appt.id}
                  className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:border-brand-200 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700 transition-all flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-900 dark:text-white">{appt.serviceName}</span>
                      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300">
                        <CheckCircle className="h-3 w-3" />
                        BOOKED
                      </span>
                    </div>

                    <div className="mt-3 space-y-1 text-sm text-slate-600 dark:text-slate-400">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4 text-slate-400" />
                        <span className="text-slate-900 dark:text-slate-200 font-medium">{start.date}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Clock className="h-4 w-4 text-slate-400" />
                        <span>{start.time} – {end.time}</span>
                      </div>
                    </div>

                    {appt.reason && (
                      <p className="mt-3 text-xs text-slate-500 dark:text-slate-400 italic bg-slate-50 dark:bg-slate-800/60 p-2 rounded border border-slate-100 dark:border-slate-800">
                        "{appt.reason}"
                      </p>
                    )}
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-end">
                    <Link
                      to="/my-appointments"
                      className="text-xs font-semibold text-brand-600 dark:text-brand-400 hover:underline"
                    >
                      Manage &rarr;
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Past/Recent Appointments Section */}
      {pastAppointments.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">Recent Activity</h2>
          <div className="rounded-xl border border-slate-200 bg-white divide-y divide-slate-100 overflow-hidden shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:divide-slate-800">
            {pastAppointments.map((appt) => {
              const start = formatDateTime(appt.startTime);
              return (
                <div key={appt.id} className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-lg ${appt.status === 'BOOKED' ? 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300' : 'bg-red-50 text-red-600 dark:bg-red-950/60 dark:text-red-400'}`}>
                      {appt.status === 'BOOKED' ? <Calendar className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-900 dark:text-white">{appt.serviceName}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">{start.date} at {start.time}</p>
                    </div>
                  </div>
                  <span
                    className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                      appt.status === 'BOOKED' ? 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300' : 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300'
                    }`}
                  >
                    {appt.status}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
