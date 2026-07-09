<script lang="ts">
  import { adminSettings } from '../stores/settings';
  
  const currentYear = new Date().getFullYear();
  $: version = $adminSettings?.version || '0.1.0';
</script>

<footer class="h-auto md:h-12 bg-bg-surface border-t border-border-subtle flex flex-col md:flex-row items-center justify-between px-4 md:px-8 py-3 md:py-0 text-[10px] text-zinc-400 font-medium transition-colors duration-200">
  
  <div class="flex items-center gap-4 mb-2 md:mb-0">
    <span>&copy; {currentYear} <span class="hidden sm:inline">KraftAdmin Ecosystem</span></span>
    <span class="hidden sm:block h-1 w-1 bg-zinc-700 rounded-full"></span>
    <span class="uppercase tracking-widest text-zinc-500">v{version}</span>
  </div>

  <div class="flex flex-wrap justify-center gap-4 md:gap-8 uppercase tracking-widest">
    <a href="https://github.com/bowerzlabs/kraftadmin/issues" 
       target="_blank" 
       rel="noopener noreferrer"
       class="hover:text-brand-primary transition-colors flex items-center gap-1.5">
      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <span class="hidden sm:inline">Report Bug</span>
    </a>

    <a href="https://github.com/bowerzlabs/kraftadmin/discussions" 
       target="_blank" 
       rel="noopener noreferrer"
       class="hover:text-brand-primary transition-colors flex items-center gap-1.5">
      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
      </svg>
      <span class="hidden sm:inline">Support</span>
    </a>

    <a href="/#/license" class="hover:text-brand-primary transition-colors">License</a>
  </div>
</footer>