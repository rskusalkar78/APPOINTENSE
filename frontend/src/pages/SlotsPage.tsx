import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { AppointmentSlotItem, ServiceItem, AppointmentItem, ApiErrorResponse } from '../types';
import { 
  Calendar, 
  Clock, 
  CheckCircle2, 
  AlertCircle, 
  Check, 
  Layers,
  ChevronRight,
  X
} from 'lucide-react';
import { AxiosError } from 'axios';

export const SlotsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const serviceIdParam = searchParams.get('serviceId');

  const [slots, setSlots] = useState<AppointmentSlotItem[]>([]);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<string>(serviceIdParam || '');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Booking modal state
  const [selectedSlot, setSelectedSlot] = useState<AppointmentSlotItem | null>(null);
  const [reason, setReason] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState<string | null>(null);
  const [bookingError, setBookingError] = useState<string | null>(null);

  // Fetch active services for filter dropdown
  useEffect(() => {
    const fetchServices = async () => {
      try {
        const res = await api.get<ServiceItem[]>('/services');
        setServices(res.data);
      } catch (e) {
        console.error('Failed to load services for filter', e);
      }
    };
    fetchServices();
  }, []);

  // Sync selectedServiceId if URL query changes
  useEffect(() => {
    if (serviceIdParam !== null) {
      setSelectedServiceId(serviceIdParam);
    }
  }, [serviceIdParam]);

  const fetchSlots = async () => {
    setLoading(true);
    setError(null);
    try {
      const params: Record<string, string> = {};
      if (selectedServiceId) {
        params.serviceId = selectedServiceId;
      }
      if (fromDate) {
        params.from = `${fromDate}T00:00:00`;
      }
      if (toDate) {
        params.to = `${toDate}T23:59:59`;
      }

      const response = await api.get<AppointmentSlotItem[]>('/slots', { params });
      setSlots(response.data);
    } catch (err) {
      const error = err as AxiosError<ApiErrorResponse>;
      setError(error.response?.data?.message || 'Failed to fetch appointment slots.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSlots();
  }, [selectedServiceId, fromDate, toDate]);

  const handleFilterChange = (serviceId: string) => {
    setSelectedServiceId(serviceId);
    if (serviceId) {
      setSearchParams({ serviceId });
    } else {
      setSearchParams({});
    }
  };

  const handleOpenBookingModal = (slot: AppointmentSlotItem) => {
    if (!isAuthenticated) {
      navigate('/login?redirect=/slots');
      return;
    }
    setSelectedSlot(slot);
    setReason('');
    setBookingError(null);
    setBookingSuccess(null);
  };

  const handleCloseBookingModal = () => {
    setSelectedSlot(null);
    setReason('');
    setBookingError(null);
    setBookingSuccess(null);
  };

  const handleConfirmBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSlot) return;

    setBookingLoading(true);
    setBookingError(null);
    setBookingSuccess(null);

    try {
      await api.post<AppointmentItem>('/appointments', {
        userId: user?.id,
        slotId: selectedSlot.id,
        reason: reason.trim() || undefined,
      });

      setBookingSuccess('Appointment confirmed successfully!');
      setTimeout(() => {
        handleCloseBookingModal();
        fetchSlots();
      }, 1500);
    } catch (err) {
      const error = err as AxiosError<any>;
      console.error('Booking submission error:', error);
      if (error.response?.status === 409) {
        setBookingError('This slot was just booked by another user. Please choose another slot.');
      } else if (error.response?.data?.validationErrors && error.response.data.validationErrors.length > 0) {
        const fieldMsgs = error.response.data.validationErrors.map((v: any) => `${v.field}: ${v.message}`).join(', ');
        setBookingError(fieldMsgs);
      } else if (error.response?.data?.message) {
        setBookingError(error.response.data.message);
      } else if (error.response?.status === 401) {
        setBookingError('Authentication expired. Please log out and sign in again.');
      } else if (error.response?.status === 403) {
        setBookingError('You do not have permission to book this appointment.');
      } else if (error.message) {
        setBookingError(error.message);
      } else {
        setBookingError('Failed to complete booking. Please try again.');
      }
    } finally {
      setBookingLoading(false);
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
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Available Appointment Slots</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Select an open consultation slot with your preferred medical or professional service.
          </p>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label htmlFor="service-filter" className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
              Filter by Service
            </label>
            <div className="relative">
              <select
                id="service-filter"
                value={selectedServiceId}
                onChange={(e) => handleFilterChange(e.target.value)}
                className="block w-full rounded-lg border border-slate-300 py-2 pl-3 pr-8 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
              >
                <option value="">All Services</option>
                {services.map((s) => (
                  <option key={s.id} value={s.id.toString()}>
                    {s.name} ({s.durationMinutes} min)
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="from-filter" className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
              From Date
            </label>
            <input
              id="from-filter"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>

          <div>
            <label htmlFor="to-filter" className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
              To Date
            </label>
            <input
              id="to-filter"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="block w-full rounded-lg border border-slate-300 py-2 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>
        </div>

        {(selectedServiceId || fromDate || toDate) && (
          <div className="mt-3 flex items-center justify-between border-t border-slate-100 dark:border-slate-800 pt-3">
            <span className="text-xs text-slate-500 dark:text-slate-400">Filters applied</span>
            <button
              onClick={() => {
                setSelectedServiceId('');
                setFromDate('');
                setToDate('');
                setSearchParams({});
              }}
              className="text-xs font-semibold text-brand-600 dark:text-brand-400 hover:underline"
            >
              Clear all filters
            </button>
          </div>
        )}
      </div>

      {/* Error state */}
      {error && (
        <div className="flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
          <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0" />
          <div className="flex-1">
            <p className="font-semibold">Error Loading Slots</p>
            <p className="text-xs text-red-600 dark:text-red-400 mt-0.5">{error}</p>
          </div>
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <div key={n} className="rounded-xl border border-slate-200 bg-white p-5 animate-pulse space-y-3 dark:border-slate-800 dark:bg-slate-900">
              <div className="h-5 w-3/4 bg-slate-200 dark:bg-slate-800 rounded" />
              <div className="h-4 w-1/2 bg-slate-100 dark:bg-slate-850 rounded" />
              <div className="h-8 w-full bg-slate-200 dark:bg-slate-800 rounded-lg pt-2" />
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && slots.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center dark:border-slate-800 dark:bg-slate-900">
          <Clock className="mx-auto h-12 w-12 text-slate-400" />
          <h3 className="mt-3 text-base font-semibold text-slate-900 dark:text-white">No Open Slots Found</h3>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {selectedServiceId || fromDate || toDate
              ? 'Try adjusting your filters or date range.'
              : 'There are currently no bookable slots scheduled. Please check back later.'}
          </p>
        </div>
      )}

      {/* Slots Grid */}
      {!loading && !error && slots.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {slots.map((slot) => {
            const start = formatDateTime(slot.startTime);
            const end = formatDateTime(slot.endTime);

            return (
              <div
                key={slot.id}
                className="flex flex-col justify-between rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:border-brand-300 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700 transition-all"
              >
                <div>
                  <div className="flex items-center justify-between gap-2">
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-brand-700 bg-brand-50 px-2.5 py-1 rounded-md dark:bg-brand-950/60 dark:text-brand-300">
                      <Layers className="h-3.5 w-3.5" />
                      {slot.serviceName}
                    </span>
                    <span className="text-xs text-slate-400 font-medium">
                      {slot.serviceDurationMinutes}m
                    </span>
                  </div>

                  <div className="mt-4 space-y-1.5">
                    <div className="flex items-center gap-2 text-sm font-semibold text-slate-900 dark:text-white">
                      <Calendar className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                      <span>{start.date}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
                      <Clock className="h-4 w-4 text-slate-400 dark:text-slate-500" />
                      <span>{start.time} – {end.time}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-5 pt-3 border-t border-slate-100 dark:border-slate-800">
                  <button
                    onClick={() => handleOpenBookingModal(slot)}
                    className="w-full inline-flex items-center justify-center gap-1.5 rounded-lg bg-brand-600 py-2 px-3 text-sm font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-500 transition-colors"
                  >
                    <span>Book This Slot</span>
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Booking Confirmation Dialog Modal */}
      {selectedSlot && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl space-y-4 dark:bg-slate-900 dark:border dark:border-slate-800">
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Confirm Appointment</h3>
              <button
                onClick={handleCloseBookingModal}
                disabled={bookingLoading}
                className="rounded-lg p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 focus:outline-none"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {bookingSuccess && (
              <div className="flex items-center gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3.5 text-sm text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
                <CheckCircle2 className="h-5 w-5 text-emerald-600 dark:text-emerald-400 flex-shrink-0" />
                <span>{bookingSuccess}</span>
              </div>
            )}

            {bookingError && (
              <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-3.5 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300">
                <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
                <span>{bookingError}</span>
              </div>
            )}

            <div className="rounded-xl bg-slate-50 p-4 space-y-2 border border-slate-200 dark:border-slate-800 dark:bg-slate-800/60">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-slate-400">Service:</span>
                <span className="font-semibold text-slate-900 dark:text-white">{selectedSlot.serviceName}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-slate-400">Date:</span>
                <span className="font-medium text-slate-900 dark:text-white">{formatDateTime(selectedSlot.startTime).date}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-slate-400">Time:</span>
                <span className="font-medium text-slate-900 dark:text-white">
                  {formatDateTime(selectedSlot.startTime).time} – {formatDateTime(selectedSlot.endTime).time}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-slate-400">Duration:</span>
                <span className="font-medium text-slate-900 dark:text-white">{selectedSlot.serviceDurationMinutes} minutes</span>
              </div>
            </div>

            <form onSubmit={handleConfirmBooking} className="space-y-4">
              <div>
                <label htmlFor="reason" className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Reason for Visit <span className="text-xs text-slate-400">(Optional)</span>
                </label>
                <textarea
                  id="reason"
                  rows={3}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="E.g., Routine health checkup, dental cleaning consultation..."
                  maxLength={500}
                  className="block w-full rounded-lg border border-slate-300 p-2.5 text-sm placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={handleCloseBookingModal}
                  disabled={bookingLoading}
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-750"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={bookingLoading || !!bookingSuccess}
                  className="inline-flex items-center gap-2 rounded-lg bg-brand-600 px-5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-brand-700 dark:bg-brand-500 dark:hover:bg-brand-600 disabled:opacity-60"
                >
                  {bookingLoading ? (
                    <>
                      <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      <span>Securing Slot...</span>
                    </>
                  ) : (
                    <>
                      <Check className="h-4 w-4" />
                      <span>Confirm Booking</span>
                    </>
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
