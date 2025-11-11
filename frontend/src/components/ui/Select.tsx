import * as React from 'react';

interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange'> {
  options: Array<{ value: string; label: string }>;
  onChange?: (value: string) => void;
}

const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ options, className, children, onChange, ...props }, ref) => {
    const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      onChange?.(e.target.value);
      // If there's an onChange prop in the rest of the props (which handles events), call that too
      const nativeProps = props as React.SelectHTMLAttributes<HTMLSelectElement>;
      if (nativeProps.onChange) {
        (nativeProps.onChange as React.ChangeEventHandler<HTMLSelectElement>)?.(e);
      }
    };
    
    return (
      <select
        className={`block w-full rounded-md border border-slate-300 bg-white py-2 px-3 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-brand-500 sm:text-sm ${
          className || ''
        }`}
        ref={ref}
        onChange={handleChange}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
        {children}
      </select>
    );
  }
);

Select.displayName = 'Select';

export default Select;