<script>
    import ServiceEditScreen from '$lib/components/services/ServiceEditScreen.svelte';
    import { page } from '$app/stores';

    const id = $page.params.id;
</script>

<ServiceEditScreen serviceId={id === 'new' ? null : id} />
