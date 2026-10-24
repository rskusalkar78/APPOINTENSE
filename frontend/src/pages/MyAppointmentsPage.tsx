import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { AppointmentItem, ApiErrorResponse } from '../types';
import { 
  Calendar, 
  Clock, 
  XCircle, 
  CheckCircle, 
  AlertCircle, 
  Ban,
  RefreshCw,
  X
} from 'lucide-react';
import { AxiosError } from 'axios';

export const MyAppointmentsPage: React.FC = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<AppointmentItem[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Cancellation modal state
  const [cancellingAppt, setCancellingAppt] = useState<AppointmentItem | null>(null);
  const [cancellationReason, setCancellationReason] = useState('');
  const [cancelLoading, setCancelLoading] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const fetchAppointments = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const params: Record<string, string> = {};
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      const response = await api.get<AppointmentItem[]>(`/appointments/user/${user.id}`, { params });
      setAppointments(response.data);
    } catch (err) {
      const error = err as AxiosError<ApiErrorResponse>;
      setError(error.response?.data?.message || 'Failed to fetch appointments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAppointments();
  }, [user, statusFilter]);

  const handleOpenCancelModal = (appt: AppointmentItem) => {
    setCancellingAppt(appt);
    setCancellationReason('');
    setCancelError(null);
  };

  const handleCloseCancelModal = () => {
    setCancellingAppt(null);
    setCancellationReason('');
    setCancelError(null);
  };

  const handleConfirmCancel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancellingAppt) return;

    setCancelLoading(true);
    setCancelError(null);

    try {
      await api.post(`/appointments/${cancellingAppt.id}/cancel`, {
        cancellationReason: cancellationReason.trim() || undefined,
      });

      handleCloseCancelModal();
      fetchAppointments();
    } catch (err) {
      const error = err as AxiosError<ApiErrorResponse>;
      setCancelError(error.response?.data?.message || 'Failed to cancel appointment. Please try again.');
    } finally {
      setCancelLoading(false);
    }
  };

  const formatDateTime = (isoString: string) => {
    const d = new Date(isoString);
    return {
      date: d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 dark:border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">My Appointments</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            View booking status, schedule dates, and manage or cancel reservations.
          </p>
        </div>

        {/* Filter buttons */}
        <div className="flex items-center gap-2">
          <div className="inline-flex rounded-lg border border-slate-200 bg-white p-1 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            {(['ALL', 'BOOKED', 'CANCELLED'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setStatusFilter(tab)}
                className={`rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
                  statusFilter === tab
                    ? 'bg-brand-600 text-white shadow-sm dark:bg-brand-500'
                    : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
                }`}
              >
                {tab === 'ALL' ? 'All' : tab === 'BOOKED' ? 'Booked' : 'Cancelled'}
              </button>
            ))}
          </div>

          <button
            onClick={fetchAppointments}
            disabled={loading}
            className="rounded-lg border border-slate-200 bg-white p-2 text-slate-600 shadow-sm hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 disabled:opacity-50 transition-colors"
            title="Refresh appointments"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
          <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0" />
          <p>{error}</p>
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="space-y-4">
          {[1, 2, 3].map((n) => (
            <div key={n} className="h-32 rounded-xl border border-slate-200 bg-white p-5 animate-pulse dark:border-slate-800 dark:bg-slate-900" />
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && appointments.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center dark:border-slate-800 dark:bg-slate-900">
          <Calendar className="mx-auto h-12 w-12 text-slate-400" />
          <h3 className="mt-3 text-base font-semibold text-slate-900 dark:text-white">No Appointments Found</h3>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {statusFilter !== 'ALL'
              ? `You have no appointments with status "${statusFilter}".`
              : "You haven't scheduled any appointments yet."}
          </p>
        </div>
      )}

      {/* Appointments List */}
      {!loading && !error && appointments.length > 0 && (
        <div className="space-y-4">
          {appointments.map((appt) => {
            const start = formatDateTime(appt.startTime);
            const end = formatDateTime(appt.endTime);
            const isBooked = appt.status === 'BOOKED';

            return (
              <div
                key={appt.id}
                className="flex flex-col md:flex-row md:items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm hover:border-slate-300 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700 transition-all"
              >
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-lg font-bold text-slate-900 dark:text-white">{appt.serviceName}</span>
                    <span
                      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                        isBooked
                          ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300'
                          : 'bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-400'
                      }`}
                    >
                      {isBooked ? (
                        <>
                          <CheckCircle className="h-3 w-3" />
                          BOOKED
                        </>
                      ) : (
                        <>
                          <XCircle className="h-3 w-3" />
                          CANCELLED
                        </>
                      )}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-sm text-slate-600 dark:text-slate-400">
                    <div className="flex items-center gap-1.5 font-medium text-slate-800 dark:text-slate-200">
                      <Calendar className="h-4 w-4 text-brand-600 dark:text-brand-400" />
                      <span>{start.date}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Clock className="h-4 w-4 text-slate-400" />
                      <span>{start.time} – {end.time}</span>
                    </div>
                    <span className="text-slate-300 dark:text-slate-700">•</span>
                    <span className="text-xs text-slate-500 dark:text-slate-500">Ref #{appt.id}</span>
                  </div>

                  {appt.reason && (
                    <p className="text-xs text-slate-500 dark:text-slate-400 italic max-w-xl">
                      Reason / Note: {appt.reason}
                    </p>
                  )}
                </div>

                {/* Cancel action */}
                <div className="flex items-center gap-3 pt-3 md:pt-0 border-t md:border-t-0 border-slate-100 dark:border-slate-800">
                  {isBooked ? (
                    <button
                      onClick={() => handleOpenCancelModal(appt)}
                      className="inline-flex items-center gap-1.5 rounded-lg border border-red-200 bg-white px-3.5 py-2 text-xs font-semibold text-red-600 shadow-sm hover:bg-red-50 hover:border-red-300 dark:border-red-900/50 dark:bg-slate-800 dark:text-red-400 dark:hover:bg-red-950/40 focus:outline-none focus:ring-2 focus:ring-red-500 transition-colors"
                    >
                      <Ban className="h-3.5 w-3.5" />
                      Cancel Appointment
                    </button>
                  ) : (
                    <span className="text-xs text-slate-400 dark:text-slate-500 italic">No actions available</span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Cancellation Dialog Modal */}
      {cancellingAppt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl space-y-4 dark:bg-slate-900 dark:border dark:border-slate-800">
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white text-red-600 flex items-center gap-2">
                <Ban className="h-5 w-5" />
                Cancel Appointment
              </h3>
              <button
                onClick={handleCloseCancelModal}
                disabled={cancelLoading}
                className="rounded-lg p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 focus:outline-none"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {cancelError && (
              <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-3 text-xs text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
                <AlertCircle className="h-4 w-4 text-red-500 flex-shrink-0 mt-0.5" />
                <span>{cancelError}</span>
              </div>
            )}

            <p className="text-sm text-slate-600 dark:text-slate-300">
              Are you sure you want to cancel your appointment for{' '}
              <strong className="text-slate-900 dark:text-white">{cancellingAppt.serviceName}</strong> on{' '}
              <strong className="text-slate-900 dark:text-white">{formatDateTime(cancellingAppt.startTime).date}</strong>?
            </p>

            <form onSubmit={handleConfirmCancel} className="space-y-4">
              <div>
                <label htmlFor="cancel-reason" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase mb-1">
                  Cancellation Reason <span className="text-slate-400 font-normal">(Optional)</span>
                </label>
                <textarea
                  id="cancel-reason"
                  rows={2}
                  value={cancellationReason}
                  onChange={(e) => setCancellationReason(e.target.value)}
                  placeholder="E.g., Scheduling conflict, feeling better..."
                  maxLength={255}
                  className="block w-full rounded-lg border border-slate-300 p-2.5 text-sm placeholder:text-slate-400 focus:border-red-500 focus:outline-none focus:ring-1 focus:ring-red-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={handleCloseCancelModal}
                  disabled={cancelLoading}
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-750"
                >
                  Keep Appointment
                </button>
                <button
                  type="submit"
                  disabled={cancelLoading}
                  className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-700 disabled:opacity-60"
                >
                  {cancelLoading ? (
                    <span className="flex items-center gap-2">
                      <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      Cancelling...
                    </span>
                  ) : (
                    'Confirm Cancellation'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
