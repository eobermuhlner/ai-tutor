import { useEffect, useState, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import Layout from '../components/layout/Layout';
import UserTable from '../components/admin/UserTable';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import type { User } from '../types';
import {
  getUsers,
  updateUser,
  type GetUsersParams,
} from '../api/admin';
import { Users as UsersIcon, Search, Filter, ChevronLeft, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';

export default function AdminUsersPage() {
  const { user: currentUser } = useAuthStore();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(20);

  // Filter state
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState<'USER' | 'ADMIN' | 'EDITOR' | ''>('');
  const [subscriptionFilter, setSubscriptionFilter] = useState<'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10' | ''>('');
  const [enabledFilter, setEnabledFilter] = useState<'true' | 'false' | ''>('');
  const [lockedFilter, setLockedFilter] = useState<'true' | 'false' | ''>('');
  const [showFilters, setShowFilters] = useState(false);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const params: GetUsersParams = {
        page: currentPage,
        size: pageSize,
      };

      if (searchTerm) params.search = searchTerm;
      if (roleFilter) params.role = roleFilter;
      if (subscriptionFilter) params.subscriptionPlan = subscriptionFilter;
      if (enabledFilter) params.enabled = enabledFilter === 'true';
      if (lockedFilter) params.locked = lockedFilter === 'true';

      const response = await getUsers(params);
      setUsers(response.users);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Error fetching users:', error);
      toast.error('Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, searchTerm, roleFilter, subscriptionFilter, enabledFilter, lockedFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const isAdmin = currentUser?.roles.includes('ADMIN') || false;
  const isLoggedIn = !!currentUser;

  // Check access after all hooks have been called
  if (isLoggedIn && !isAdmin) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto p-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-red-800">Access Denied</h2>
            <p className="text-red-600">
              You must be an administrator to access user management.
            </p>
          </div>
        </div>
      </Layout>
    );
  }

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0); // Reset to first page when searching
    fetchUsers();
  };

  const handleQuickAction = async (userId: string, action: 'enable' | 'disable' | 'lock' | 'unlock') => {
    try {
      const updateData: { enabled?: boolean; locked?: boolean } = {};

      if (action === 'enable') {
        updateData.enabled = true;
      } else if (action === 'disable') {
        updateData.enabled = false;
      } else if (action === 'lock') {
        updateData.locked = true;
      } else if (action === 'unlock') {
        updateData.locked = false;
      }

      await updateUser(userId, updateData);
      toast.success(`User ${action}d successfully`);
      fetchUsers(); // Refresh the list
    } catch (error) {
      console.error(`Error ${action}ing user:`, error);
      toast.error(`Failed to ${action} user`);
    }
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setRoleFilter('');
    setSubscriptionFilter('');
    setEnabledFilter('');
    setLockedFilter('');
    setCurrentPage(0);
  };

  const hasActiveFilters =
    searchTerm || roleFilter || subscriptionFilter || enabledFilter || lockedFilter;

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 rounded-full bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center">
            <UsersIcon className="w-7 h-7 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">User Management</h1>
            <p className="text-sm text-slate-600">
              {totalElements} total user{totalElements !== 1 ? 's' : ''}
            </p>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
          <form onSubmit={handleSearch} className="space-y-4">
            {/* Search Bar */}
            <div className="flex gap-2">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search by username, email, or name..."
                  className="w-full pl-10 pr-4 py-2.5 border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                />
              </div>
              <Button type="submit" size="md">
                Search
              </Button>
              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={() => setShowFilters(!showFilters)}
              >
                <Filter className="w-4 h-4 mr-2" />
                {showFilters ? 'Hide' : 'Show'} Filters
              </Button>
            </div>

            {/* Advanced Filters */}
            {showFilters && (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 pt-4 border-t border-slate-200">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Role
                  </label>
                  <select
                    value={roleFilter}
                    onChange={(e) => setRoleFilter(e.target.value as 'USER' | 'ADMIN' | 'EDITOR' | '')}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  >
                    <option value="">All Roles</option>
                    <option value="USER">User</option>
                    <option value="ADMIN">Admin</option>
                    <option value="EDITOR">Editor</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Subscription
                  </label>
                  <select
                    value={subscriptionFilter}
                    onChange={(e) => setSubscriptionFilter(e.target.value as 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10' | '')}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  >
                    <option value="">All Plans</option>
                    <option value="FREE">Free</option>
                    <option value="FREE_BYOK">Free + BYOK</option>
                    <option value="SUBSCRIPTION_10">Premium</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Enabled
                  </label>
                  <select
                    value={enabledFilter}
                    onChange={(e) => setEnabledFilter(e.target.value as 'true' | 'false' | '')}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  >
                    <option value="">All</option>
                    <option value="true">Enabled</option>
                    <option value="false">Disabled</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Locked
                  </label>
                  <select
                    value={lockedFilter}
                    onChange={(e) => setLockedFilter(e.target.value as 'true' | 'false' | '')}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  >
                    <option value="">All</option>
                    <option value="false">Unlocked</option>
                    <option value="true">Locked</option>
                  </select>
                </div>
              </div>
            )}

            {/* Clear Filters */}
            {hasActiveFilters && (
              <div className="flex justify-end">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleClearFilters}
                >
                  Clear all filters
                </Button>
              </div>
            )}
          </form>
        </div>

        {/* User Table */}
        <div className="bg-white rounded-2xl shadow-soft border border-slate-200 overflow-hidden">
          {loading ? (
            <div className="flex justify-center items-center py-12">
              <Spinner />
            </div>
          ) : (
            <UserTable users={users} onQuickAction={handleQuickAction} />
          )}
        </div>

        {/* Pagination */}
        {!loading && totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between">
            <div className="text-sm text-slate-600">
              Showing {currentPage * pageSize + 1}-
              {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements}
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={currentPage === 0}
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                Previous
              </Button>
              <div className="flex items-center gap-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  // Show first page, current page ± 1, and last page
                  let pageNum;
                  if (totalPages <= 5) {
                    pageNum = i;
                  } else if (currentPage < 3) {
                    pageNum = i;
                  } else if (currentPage > totalPages - 4) {
                    pageNum = totalPages - 5 + i;
                  } else {
                    pageNum = currentPage - 2 + i;
                  }

                  return (
                    <button
                      key={i}
                      onClick={() => setCurrentPage(pageNum)}
                      className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                        pageNum === currentPage
                          ? 'bg-brand-600 text-white'
                          : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-300'
                      }`}
                    >
                      {pageNum + 1}
                    </button>
                  );
                })}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={currentPage >= totalPages - 1}
              >
                Next
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
