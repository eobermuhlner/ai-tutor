import CopyButton from './CopyButton';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownMessageProps {
    content: string;
}

export default function MarkdownMessage({ content }: MarkdownMessageProps) {
    return (
        <div className="prose prose-slate max-w-none relative group">
            <div className="absolute -top-2 -right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 focus-within:opacity-100">
                <CopyButton text={content} title="Copy message" />
            </div>
            <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                    p: ({ children }) => <p className="mb-1 last:mb-0 leading-relaxed">{children}</p>,
                    h1: ({ children }) => <h1 className="text-2xl font-bold mt-6 mb-4 text-slate-800">{children}</h1>,
                    h2: ({ children }) => <h2 className="text-xl font-bold mt-5 mb-3 text-slate-700">{children}</h2>,
                    h3: ({ children }) => <h3 className="text-lg font-semibold mt-4 mb-2 text-slate-600">{children}</h3>,
                    ul: ({ children }) => <ul className="list-disc list-inside my-1 space-y-px pl-5">{children}</ul>,
                    ol: ({ children }) => <ol className="list-decimal list-inside my-1 space-y-px pl-5">{children}</ol>,
                    li: ({ children }) => <li className="mb-0 leading-tight pl-1">{children}</li>,
                    blockquote: ({ children }) => (
                        <blockquote className="border-l-4 border-blue-200 pl-3 text-slate-600 my-2 py-0.5 bg-blue-50 rounded-r-md">
                            {children}
                        </blockquote>
                    ),
                    code: (({ inline, ...props }: { inline?: boolean } & Record<string, any>) => {
                      if (inline) {
                        return (
                          <code className="bg-slate-100 text-slate-800 px-1 py-0.5 rounded text-sm font-mono border border-slate-200" {...props as any}>
                              {props.children}
                          </code>
                        );
                      } else {
                        return (
                          <pre className="bg-slate-900 text-slate-100 p-2.5 rounded-lg overflow-x-auto my-2 text-sm border border-slate-300">
                            <code {...props as any}>{props.children}</code>
                          </pre>
                        );
                      }
                    }) as any,
                    a: ({ children, href }) => (
                        <a href={href} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:text-blue-800 underline">
                            {children}
                        </a>
                    ),
                    strong: ({ children }) => <strong className="font-semibold text-slate-800">{children}</strong>,
                    em: ({ children }) => <em className="italic text-slate-700">{children}</em>,
                    hr: () => <hr className="border-slate-200 my-3" />,
                }}
            >
                {content}
            </ReactMarkdown>
        </div>
    );
}
