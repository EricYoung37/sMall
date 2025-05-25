FROM cassandra:latest

# Copy the entrypoint script
COPY cassandra-entrypoint.sh /usr/local/bin/cassandra-entrypoint.sh

# Make it executable
RUN chmod +x /usr/local/bin/cassandra-entrypoint.sh

# Override the default entrypoint
ENTRYPOINT ["/usr/local/bin/cassandra-entrypoint.sh"]

# Default command to start Cassandra (same as official image)
CMD ["cassandra", "-f"]