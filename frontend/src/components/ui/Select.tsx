import * as React from 'react';

interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange'> {
  label?: string;
  options?: Array<{ value: string; label: string }>;
  onChange?: (value: string) => void;
  error?: string;
}

const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, className, children, onChange, error, ...props }, ref) => {
    const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      onChange?.(e.target.value);
      // If there's an onChange prop in the rest of the props (which handles events), call that too
      const nativeProps = props as React.SelectHTMLAttributes<HTMLSelectElement>;
      if (nativeProps.onChange) {
        (nativeProps.onChange as React.ChangeEventHandler<HTMLSelectElement>)?.(e);
      }
    };

    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-medium text-slate-700 mb-1">
            {label}
            {props.required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <select
          className={`block w-full rounded-md border ${
            error ? 'border-red-300' : 'border-slate-300'
          } bg-white py-2 px-3 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-brand-500 sm:text-sm ${
            className || ''
          }`}
          ref={ref}
          onChange={handleChange}
          {...props}
        >
          {options
            ? options.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))
            : children}
        </select>
        {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';

export default Select;