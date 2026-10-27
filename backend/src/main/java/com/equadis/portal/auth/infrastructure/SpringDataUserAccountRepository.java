package com.equadis.portal.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.equadis.portal.auth.domain.UserAccount;
import com.equadis.portal.auth.domain.UserAccountRepository;

/** Spring Data adapter for the {@link UserAccountRepository} port. */
interface SpringDataUserAccountRepository extends JpaRepository<UserAccount, Long>, UserAccountRepository {

}
